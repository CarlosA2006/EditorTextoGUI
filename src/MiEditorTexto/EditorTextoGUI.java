package MiEditorTexto;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder; // Nuevo import para márgenes
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.List; // Necesario para SwingWorker

public class EditorTextoGUI extends JFrame {

    private JTextPane textPane;
    private JLabel statusLabel;

    // --- NUEVO: Referencia al componente visual propio ---
    private ProgressLabel progressLabel;
    // ----------------------------------------------------

    private UndoManager undoManager;
    private File currentFile;

    public EditorTextoGUI() {
        super("Editor/Conversor de Texto con GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600); // Un poco más ancho para que quepa la barra
        setLocationRelativeTo(null);

        initComponents();
        createMenuBar();
        createPopupMenu();
    }

    private void initComponents() {
        textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textPane);
        add(scrollPane, BorderLayout.CENTER);

        // --- MODIFICACIÓN: Barra de estado compuesta ---
        // En lugar de añadir solo el statusLabel al Sur, creamos un panel contenedor
        // para tener el contador a la izquierda y el ProgressLabel a la derecha.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        statusLabel = new JLabel("Caracteres: 0 | Palabras: 0 | Líneas: 0");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Instanciamos el componente propio
        progressLabel = new ProgressLabel();
        // Lo añadimos al panel inferior
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(progressLabel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
        // -----------------------------------------------

        undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateStatus(); }
            @Override public void removeUpdate(DocumentEvent e) { updateStatus(); }
            @Override public void changedUpdate(DocumentEvent e) { updateStatus(); }
        });
        updateStatus();
    }

    private ImageIcon loadIcon(String path) {
        try {
            return new ImageIcon(getClass().getResource("/icons/" + path));
        } catch (Exception e) {
            // System.err.println("Error loading icon: " + path); // Comentado para no ensuciar consola
            return null;
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // --- Menú Archivo ---
        JMenu fileMenu = new JMenu("Archivo");
        JMenuItem openItem = new JMenuItem("Abrir", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItem.setIcon(loadIcon("open.png"));
        openItem.addActionListener(e -> openFile());

        JMenuItem saveItem = new JMenuItem("Guardar", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.setIcon(loadIcon("save.png"));
        // --- MODIFICACIÓN: Usamos saveFileWithProgress para cumplir los requisitos visuales ---
        saveItem.addActionListener(e -> saveFileWithProgress());

        JMenuItem saveAsItem = new JMenuItem("Guardar como...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.setIcon(loadIcon("save_as.png"));
        saveAsItem.addActionListener(e -> saveFileAs());

        JMenuItem exitItem = new JMenuItem("Salir", KeyEvent.VK_Q);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.setIcon(loadIcon("exit.png"));
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // --- Menú Edición (Código original intacto) ---
        JMenu editMenu = new JMenu("Edición");
        JMenuItem cutItem = new JMenuItem("Cortar", KeyEvent.VK_X);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        cutItem.setIcon(loadIcon("cut.png"));
        cutItem.addActionListener(e -> textPane.cut());

        JMenuItem copyItem = new JMenuItem("Copiar", KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
        copyItem.setIcon(loadIcon("copy.png"));
        copyItem.addActionListener(e -> textPane.copy());

        JMenuItem pasteItem = new JMenuItem("Pegar", KeyEvent.VK_V);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        pasteItem.setIcon(loadIcon("paste.png"));
        pasteItem.addActionListener(e -> textPane.paste());

        JMenuItem undoItem = new JMenuItem("Deshacer", KeyEvent.VK_Z);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoItem.setIcon(loadIcon("undo.png"));
        undoItem.addActionListener(e -> {
            try { undoManager.undo(); } catch (CannotUndoException ex) { Toolkit.getDefaultToolkit().beep(); }
        });

        JMenuItem redoItem = new JMenuItem("Rehacer", KeyEvent.VK_Y);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        redoItem.setIcon(loadIcon("redo.png"));
        redoItem.addActionListener(e -> {
            try { undoManager.redo(); } catch (CannotRedoException ex) { Toolkit.getDefaultToolkit().beep(); }
        });

        JMenuItem selectAllItem = new JMenuItem("Seleccionar todo", KeyEvent.VK_A);
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
        selectAllItem.setIcon(loadIcon("select_all.png"));
        selectAllItem.addActionListener(e -> textPane.selectAll());

        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);

        // --- Menú Transformaciones (Código original intacto) ---
        JMenu transformMenu = new JMenu("Transformaciones");
        JMenuItem upperCaseItem = new JMenuItem("Mayúsculas");
        upperCaseItem.addActionListener(e -> {
            try {
                Document doc = textPane.getDocument();
                String selectedText = textPane.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    int start = textPane.getSelectionStart();
                    int end = textPane.getSelectionEnd();
                    doc.remove(start, end - start);
                    doc.insertString(start, selectedText.toUpperCase(), null);
                } else {
                    String fullText = textPane.getText();
                    textPane.setText(fullText.toUpperCase());
                }
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });

        JMenuItem lowerCaseItem = new JMenuItem("Minúsculas");
        lowerCaseItem.addActionListener(e -> {
            try {
                Document doc = textPane.getDocument();
                String selectedText = textPane.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    int start = textPane.getSelectionStart();
                    int end = textPane.getSelectionEnd();
                    doc.remove(start, end - start);
                    doc.insertString(start, selectedText.toLowerCase(), null);
                } else {
                    String fullText = textPane.getText();
                    textPane.setText(fullText.toLowerCase());
                }
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });

        JMenuItem invertCaseItem = new JMenuItem("Invertir May/Min");
        invertCaseItem.addActionListener(e -> {
            try {
                Document doc = textPane.getDocument();
                String targetText;
                int start = 0;
                int end = doc.getLength();

                String selectedText = textPane.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    targetText = selectedText;
                    start = textPane.getSelectionStart();
                    end = textPane.getSelectionEnd();
                } else {
                    targetText = textPane.getText();
                }

                StringBuilder invertedText = new StringBuilder();
                for (char c : targetText.toCharArray()) {
                    if (Character.isUpperCase(c)) {
                        invertedText.append(Character.toLowerCase(c));
                    } else if (Character.isLowerCase(c)) {
                        invertedText.append(Character.toUpperCase(c));
                    } else {
                        invertedText.append(c);
                    }
                }
                doc.remove(start, end - start);
                doc.insertString(start, invertedText.toString(), null);
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });

        JMenuItem removeDoubleSpacesItem = new JMenuItem("Eliminar Espacios Dobles");
        removeDoubleSpacesItem.addActionListener(e -> {
            try {
                Document doc = textPane.getDocument();
                String fullText = doc.getText(0, doc.getLength());
                String cleanedText = fullText.replaceAll(" +", " ").trim();
                textPane.setText(cleanedText);
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });

        transformMenu.add(upperCaseItem);
        transformMenu.add(lowerCaseItem);
        transformMenu.add(invertCaseItem);
        transformMenu.add(removeDoubleSpacesItem);

        // --- Menú Formato (Código original intacto) ---
        JMenu formatMenu = new JMenu("Formato");
        JMenuItem boldItem = new JMenuItem("Negrita", KeyEvent.VK_B);
        boldItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK));
        boldItem.setIcon(loadIcon("bold.png"));
        boldItem.addActionListener(e -> applyStyle(StyleConstants.Bold));

        JMenuItem italicItem = new JMenuItem("Cursiva", KeyEvent.VK_I);
        italicItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
        italicItem.setIcon(loadIcon("italic.png"));
        italicItem.addActionListener(e -> applyStyle(StyleConstants.Italic));

        JMenuItem underlineItem = new JMenuItem("Subrayado", KeyEvent.VK_U);
        underlineItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK));
        underlineItem.setIcon(loadIcon("underline.png"));
        underlineItem.addActionListener(e -> applyStyle(StyleConstants.Underline));

        JMenuItem colorItem = new JMenuItem("Color de fuente...");
        colorItem.setIcon(loadIcon("color_text.png"));
        colorItem.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Elige un Color", textPane.getForeground());
            if (newColor != null) {
                MutableAttributeSet attrs = new SimpleAttributeSet(textPane.getCharacterAttributes());
                StyleConstants.setForeground(attrs, newColor);
                textPane.setCharacterAttributes(attrs, false);
            }
        });

        formatMenu.add(boldItem);
        formatMenu.add(italicItem);
        formatMenu.add(underlineItem);
        formatMenu.addSeparator();
        formatMenu.add(colorItem);

        // --- Menú Herramientas (Código original intacto) ---
        JMenu toolsMenu = new JMenu("Herramientas");
        JMenuItem findReplaceItem = new JMenuItem("Buscar y Reemplazar...", KeyEvent.VK_F);
        findReplaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        findReplaceItem.setIcon(loadIcon("search.png"));
        findReplaceItem.addActionListener(e -> showFindReplaceDialog());
        toolsMenu.add(findReplaceItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(transformMenu);
        menuBar.add(formatMenu);
        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    private void createPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem cutPopup = new JMenuItem("Cortar");
        cutPopup.setIcon(loadIcon("cut.png"));
        JMenuItem copyPopup = new JMenuItem("Copiar");
        copyPopup.setIcon(loadIcon("copy.png"));
        JMenuItem pastePopup = new JMenuItem("Pegar");
        pastePopup.setIcon(loadIcon("paste.png"));
        JMenuItem selectAllPopup = new JMenuItem("Seleccionar todo");
        selectAllPopup.setIcon(loadIcon("select_all.png"));

        cutPopup.addActionListener(e -> textPane.cut());
        copyPopup.addActionListener(e -> textPane.copy());
        pastePopup.addActionListener(e -> textPane.paste());
        selectAllPopup.addActionListener(e -> textPane.selectAll());

        popupMenu.add(cutPopup);
        popupMenu.add(copyPopup);
        popupMenu.add(pastePopup);
        popupMenu.addSeparator();
        popupMenu.add(selectAllPopup);

        textPane.setComponentPopupMenu(popupMenu);
    }

    private void updateStatus() {
        String text;
        try {
            text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
        } catch (BadLocationException e) {
            text = "";
        }
        int chars = text.length();
        String[] words = text.split("\\s+");
        int numWords = 0;
        for (String word : words) {
            if (!word.trim().isEmpty()) numWords++;
        }
        int lines = textPane.getDocument().getDefaultRootElement().getElementCount();
        if (chars == 0 && lines == 1) lines = 0;

        statusLabel.setText("Caracteres: " + chars + " | Palabras: " + numWords + " | Líneas: " + lines);
    }

    private void applyStyle(Object styleConstant) {
        MutableAttributeSet attrs = new SimpleAttributeSet(textPane.getCharacterAttributes());
        boolean isApplied = false;
        if (styleConstant == StyleConstants.Bold) {
            isApplied = StyleConstants.isBold(attrs);
            StyleConstants.setBold(attrs, !isApplied);
        } else if (styleConstant == StyleConstants.Italic) {
            isApplied = StyleConstants.isItalic(attrs);
            StyleConstants.setItalic(attrs, !isApplied);
        } else if (styleConstant == StyleConstants.Underline) {
            isApplied = StyleConstants.isUnderline(attrs);
            StyleConstants.setUnderline(attrs, !isApplied);
        }
        textPane.setCharacterAttributes(attrs, false);
    }

    private void showFindReplaceDialog() {
        // (Código original intacto)
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField findField = new JTextField(25);
        JTextField replaceField = new JTextField(25);
        JCheckBox caseSensitive = new JCheckBox("Coincidir mayúsculas/minúsculas");

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Buscar:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(findField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Reemplazar con:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(replaceField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        panel.add(caseSensitive, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Buscar y Reemplazar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, loadIcon("search_large.png"));

        if (result == JOptionPane.OK_OPTION) {
            String findText = findField.getText();
            String replaceText = replaceField.getText();
            boolean matchCase = caseSensitive.isSelected();

            if (findText != null && !findText.isEmpty()) {
                String fullText;
                try {
                    fullText = textPane.getDocument().getText(0, textPane.getDocument().getLength());
                    String searchTarget = matchCase ? fullText : fullText.toLowerCase();
                    String findPattern = matchCase ? findText : findText.toLowerCase();

                    if (searchTarget.contains(findPattern)) {
                        String newText = textPane.getText();
                        if (!matchCase) {
                            newText = newText.replaceAll("(?i)" + java.util.regex.Pattern.quote(findText), java.util.regex.Matcher.quoteReplacement(replaceText));
                        } else {
                            newText = newText.replace(findText, replaceText);
                        }
                        textPane.setText(newText);
                        JOptionPane.showMessageDialog(this, "Reemplazo completado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Texto no encontrado.", "Buscar", JOptionPane.INFORMATION_MESSAGE);
                    }

                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                textPane.setText("");
                String line;
                StyledDocument doc = textPane.getStyledDocument();
                while ((line = reader.readLine()) != null) {
                    doc.insertString(doc.getLength(), line + "\n", null);
                }
                undoManager.discardAllEdits();
                setTitle("Editor/Conversor de Texto con GUI - " + currentFile.getName());
            } catch (IOException | BadLocationException ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Método original (mantengo pero ya no lo uso en el menú directamente)
    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
        } else {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                writer.write(textPane.getText());
                setTitle("Editor/Conversor de Texto con GUI - " + currentFile.getName());
                JOptionPane.showMessageDialog(this, "Archivo guardado exitosamente.", "Guardar", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- NUEVO: Método de guardado con progreso ---
    // Este método permite cumplir con el requisito de "Actualización visible del progreso".
    // Simula una tarea lenta usando SwingWorker para que la barra se llene.
    private void saveFileWithProgress() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }

        // SwingWorker permite ejecutar tareas en segundo plano sin congelar la GUI
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Actualizar estado a WORKING
                SwingUtilities.invokeLater(() -> {
                    progressLabel.setState(ProgressLabel.State.WORKING);
                    progressLabel.setStatusText("Guardando...");
                });

                String content = textPane.getText();

                // Escribimos el archivo
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                    // Simulamos que tarda un poco para ver la barra (solo para demostración)
                    for (int i = 0; i <= 100; i+=2) {
                        Thread.sleep(10); // Simulación de trabajo
                        publish(i); // Enviamos progreso
                    }
                    writer.write(content);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                // Actualizar la barra visualmente (se ejecuta en el hilo de eventos)
                int latestValue = chunks.get(chunks.size() - 1);
                progressLabel.setProgressValue(latestValue);
            }

            @Override
            protected void done() {
                try {
                    get(); // Verificar si hubo excepciones
                    // Estado DONE
                    progressLabel.setState(ProgressLabel.State.DONE);
                    setTitle("Editor/Conversor de Texto con GUI - " + currentFile.getName());

                    // Timer para volver a estado IDLE después de 2 segundos
                    Timer t = new Timer(2000, e -> progressLabel.setState(ProgressLabel.State.IDLE));
                    t.setRepeats(false);
                    t.start();

                } catch (Exception e) {
                    // Estado ERROR
                    progressLabel.setState(ProgressLabel.State.ERROR);
                    progressLabel.setStatusText("Error al guardar");
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }
    // -----------------------------------------------

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            if (!currentFile.getName().toLowerCase().endsWith(".txt")) {
                currentFile = new File(currentFile.getAbsolutePath() + ".txt");
            }
            // --- MODIFICADO: Usamos la versión con progreso ---
            saveFileWithProgress();
        }
    }

    // ===========================================================================
    // --- NUEVO: CLASE ProgressLabel (Requisito: Componente visual propio) ---
    // ===========================================================================
    public static class ProgressLabel extends JPanel {

        // Requisito: Estados internos
        public enum State {
            IDLE, WORKING, DONE, ERROR
        }

        private JLabel textLabel;
        private JProgressBar progressBar;
        private State currentState;

        public ProgressLabel() {
            // Requisito: Texto y barra integrados (FlowLayout los pone juntos)
            super(new FlowLayout(FlowLayout.LEFT, 10, 0));

            textLabel = new JLabel("Listo");
            // Requisito: Propiedades configurables (configuración inicial)
            progressBar = new JProgressBar(0, 100);
            progressBar.setPreferredSize(new Dimension(150, 15));
            progressBar.setStringPainted(true);
            progressBar.setVisible(false); // Inicialmente oculto

            add(textLabel);
            add(progressBar);

            setState(State.IDLE);
        }

        // Requisito: Propiedades configurables (Setters)
        public void setStatusText(String text) {
            textLabel.setText(text);
        }

        public void setProgressValue(int value) {
            progressBar.setValue(value);
        }

        // Requisito: Aspecto personalizable (Estilos según estado)
        public void setState(State state) {
            this.currentState = state;
            switch (state) {
                case IDLE:
                    textLabel.setText("Listo");
                    textLabel.setForeground(UIManager.getColor("Label.foreground"));
                    progressBar.setVisible(false);
                    break;
                case WORKING:
                    progressBar.setVisible(true);
                    progressBar.setValue(0);
                    textLabel.setForeground(Color.BLUE); // Personalización de color
                    break;
                case DONE:
                    progressBar.setVisible(true);
                    progressBar.setValue(100);
                    textLabel.setText("Completado");
                    textLabel.setForeground(new Color(0, 150, 0)); // Verde oscuro
                    break;
                case ERROR:
                    progressBar.setVisible(true);
                    textLabel.setText("Error");
                    textLabel.setForeground(Color.RED);
                    break;
            }
        }
    }
    // ===========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("TabbedPane.tabHeight", 30);
                UIManager.put("TitlePane.unifiedBackground", false);
                UIManager.put("OptionPane.buttonFont", UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12));

            } catch (UnsupportedLookAndFeelException ex) {
                System.err.println("Failed to initialize FlatLaf");
                ex.printStackTrace();
            }
            new EditorTextoGUI().setVisible(true);
        });
    }
}