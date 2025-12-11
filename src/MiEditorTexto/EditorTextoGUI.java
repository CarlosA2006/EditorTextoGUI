package MiEditorTexto;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// ======================================================================================
// 1. CAPA DE ABSTRACCIÓN NUI (Definiciones del PDF)
// ======================================================================================

// Enum con los comandos obligatorios y opcionales
enum NuiCommand {
    NUEVO_DOCUMENTO,
    ABRIR_DOCUMENTO,
    GUARDAR_DOCUMENTO,
    APLICAR_NEGRITA,
    APLICAR_CURSIVA,
    COLOR_ROJO,
    COLOR_AZUL,
    DICTAR_TEXTO  // Opcional
}

// Interface que debe implementar la ventana para recibir órdenes
interface NuiListener {
    void onCommand(NuiCommand cmd, String payload);
}

// Controlador: Traduce texto natural (simulando voz) a comandos estructurados
class NuiController {
    private List<NuiListener> listeners = new ArrayList<>();

    public void addListener(NuiListener listener) {
        listeners.add(listener);
    }

    // "Cerebro" que interpreta el lenguaje natural
    public void processInput(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String text = input.toLowerCase().trim();
        NuiCommand cmd = null;
        String payload = "";

        // Detección de palabras clave (Keywords)
        if (text.contains("nuevo") || text.contains("borrar todo")) {
            cmd = NuiCommand.NUEVO_DOCUMENTO;
        } else if (text.contains("abrir") || text.contains("cargar")) {
            cmd = NuiCommand.ABRIR_DOCUMENTO;
        } else if (text.contains("guardar") || text.contains("salvar")) {
            cmd = NuiCommand.GUARDAR_DOCUMENTO;
        } else if (text.contains("negrita") || text.contains("fuerte")) {
            cmd = NuiCommand.APLICAR_NEGRITA;
        } else if (text.contains("cursiva") || text.contains("italica")) {
            cmd = NuiCommand.APLICAR_CURSIVA;
        } else if (text.contains("rojo")) {
            cmd = NuiCommand.COLOR_ROJO;
        } else if (text.contains("azul")) {
            cmd = NuiCommand.COLOR_AZUL;
        } else if (text.startsWith("dictar") || text.startsWith("escribir")) {
            cmd = NuiCommand.DICTAR_TEXTO;
            // Obtenemos el texto real respetando mayúsculas del input original
            int spaceIndex = input.indexOf(" ");
            if (spaceIndex != -1) {
                payload = input.substring(spaceIndex + 1);
            }
        }

        if (cmd != null) {
            notifyListeners(cmd, payload);
        } else {
            System.out.println("NUI: No entendí el comando '" + text + "'");
        }
    }

    private void notifyListeners(NuiCommand cmd, String payload) {
        for (NuiListener listener : listeners) {
            listener.onCommand(cmd, payload);
        }
    }
}

// ======================================================================================
// 2. CLASE PRINCIPAL DEL EDITOR (Modificada para implementar NuiListener)
// ======================================================================================
public class EditorTextoGUI extends JFrame implements NuiListener {

    private JTextPane textPane;
    private JLabel statusLabel;
    private ProgressLabel progressLabel; // Tu componente visual propio
    private UndoManager undoManager;
    private File currentFile;

    // Controlador NUI
    private NuiController nuiController;

    public EditorTextoGUI() {
        super("Editor/Conversor de Texto + NUI Integrado");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setLocationRelativeTo(null);

        // Inicializar NUI
        nuiController = new NuiController();
        nuiController.addListener(this); // Escuchamos los comandos

        initComponents();
        createMenuBar();
        createPopupMenu();
    }

    private void initComponents() {
        // Layout principal
        setLayout(new BorderLayout());

        // --- NUEVO: Panel Superior para Simulación NUI (Ruta A) ---
        JPanel nuiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nuiPanel.setBorder(new TitledBorder("Simulador de Voz (NUI)"));
        nuiPanel.setBackground(new Color(60, 63, 65)); // Distinguirlo visualmente

        JLabel lblSim = new JLabel("Comando de voz:");
        JTextField txtSimulacion = new JTextField(30);
        JButton btnEnviar = new JButton("Simular");
        JLabel lblAyuda = new JLabel("<html><small style='color:gray'>(Ej: 'guardar', 'poner negrita', 'color azul', 'dictar hola mundo')</small></html>");

        ActionListener sendAction = e -> {
            String command = txtSimulacion.getText();
            nuiController.processInput(command); // Enviamos al controlador
            txtSimulacion.setText("");
            txtSimulacion.requestFocus();
        };

        btnEnviar.addActionListener(sendAction);
        txtSimulacion.addActionListener(sendAction); // Al pulsar Enter

        nuiPanel.add(lblSim);
        nuiPanel.add(txtSimulacion);
        nuiPanel.add(btnEnviar);
        nuiPanel.add(lblAyuda);

        add(nuiPanel, BorderLayout.NORTH);
        // ----------------------------------------------------------

        // Panel Central (Editor)
        textPane = new JTextPane();
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textPane);
        add(scrollPane, BorderLayout.CENTER);

        // Panel Inferior (Status + ProgressLabel)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        statusLabel = new JLabel("Caracteres: 0 | Palabras: 0 | Líneas: 0");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        progressLabel = new ProgressLabel();

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(progressLabel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Lógica de deshacer y listeners de texto
        undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateStatus(); }
            @Override public void removeUpdate(DocumentEvent e) { updateStatus(); }
            @Override public void changedUpdate(DocumentEvent e) { updateStatus(); }
        });
        updateStatus();
    }

    // ===========================================================================
    // IMPLEMENTACIÓN DE LOS COMANDOS NUI (El "Puente")
    // ===========================================================================
    @Override
    public void onCommand(NuiCommand cmd, String payload) {
        // Feedback visual en el ProgressLabel
        progressLabel.setState(ProgressLabel.State.WORKING);
        progressLabel.setStatusText("Voz: " + cmd);

        System.out.println("[DEBUG NUI] Ejecutando: " + cmd);

        switch (cmd) {
            case NUEVO_DOCUMENTO:
                textPane.setText("");
                currentFile = null;
                setTitle("Editor - Nuevo");
                break;
            case ABRIR_DOCUMENTO:
                openFile();
                break;
            case GUARDAR_DOCUMENTO:
                saveFileWithProgress(); // Reutilizamos tu método con barra de progreso
                break;
            case APLICAR_NEGRITA:
                applyStyle(StyleConstants.Bold);
                break;
            case APLICAR_CURSIVA:
                applyStyle(StyleConstants.Italic);
                break;
            case COLOR_ROJO:
                applyColor(Color.RED);
                break;
            case COLOR_AZUL:
                applyColor(Color.BLUE);
                break;
            case DICTAR_TEXTO:
                try {
                    Document doc = textPane.getDocument();
                    doc.insertString(textPane.getCaretPosition(), payload + " ", null);
                } catch (BadLocationException e) { e.printStackTrace(); }
                break;
        }

        // Restaurar estado visual tras 1.5 segundos
        Timer t = new Timer(1500, e -> progressLabel.setState(ProgressLabel.State.IDLE));
        t.setRepeats(false);
        t.start();
    }

    // Método auxiliar para colores directos (NUI)
    private void applyColor(Color c) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, c);
        textPane.setCharacterAttributes(attrs, false);
    }
    // ===========================================================================

    // --- MÉTODOS DEL EDITOR ORIGINAL (Menus, Iconos, Lógica) ---

    private ImageIcon loadIcon(String path) {
        try {
            return new ImageIcon(getClass().getResource("/icons/" + path));
        } catch (Exception e) { return null; }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // --- MENU ARCHIVO ---
        JMenu fileMenu = new JMenu("Archivo");
        JMenuItem openItem = new JMenuItem("Abrir", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItem.setIcon(loadIcon("open.png"));
        openItem.addActionListener(e -> openFile());

        JMenuItem saveItem = new JMenuItem("Guardar", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.setIcon(loadIcon("save.png"));
        saveItem.addActionListener(e -> saveFileWithProgress()); // Usamos la versión con progreso

        JMenuItem saveAsItem = new JMenuItem("Guardar como...");
        saveAsItem.setIcon(loadIcon("save_as.png"));
        saveAsItem.addActionListener(e -> saveFileAs());

        JMenuItem exitItem = new JMenuItem("Salir", KeyEvent.VK_Q);
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // --- MENU EDICIÓN ---
        JMenu editMenu = new JMenu("Edición");
        JMenuItem undoItem = new JMenuItem("Deshacer", KeyEvent.VK_Z);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> { try { undoManager.undo(); } catch (CannotUndoException ex) {} });

        JMenuItem redoItem = new JMenuItem("Rehacer", KeyEvent.VK_Y);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> { try { undoManager.redo(); } catch (CannotRedoException ex) {} });

        JMenuItem cutItem = new JMenuItem("Cortar");
        cutItem.addActionListener(e -> textPane.cut());
        JMenuItem copyItem = new JMenuItem("Copiar");
        copyItem.addActionListener(e -> textPane.copy());
        JMenuItem pasteItem = new JMenuItem("Pegar");
        pasteItem.addActionListener(e -> textPane.paste());

        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(undoItem);
        editMenu.add(redoItem);

        // --- MENU FORMATO ---
        JMenu formatMenu = new JMenu("Formato");
        JMenuItem boldItem = new JMenuItem("Negrita");
        boldItem.addActionListener(e -> applyStyle(StyleConstants.Bold));
        JMenuItem italicItem = new JMenuItem("Cursiva");
        italicItem.addActionListener(e -> applyStyle(StyleConstants.Italic));
        JMenuItem colorItem = new JMenuItem("Color...");
        colorItem.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Elige Color", textPane.getForeground());
            if (newColor != null) applyColor(newColor);
        });

        formatMenu.add(boldItem);
        formatMenu.add(italicItem);
        formatMenu.add(colorItem);

        // --- MENU HERRAMIENTAS ---
        JMenu toolsMenu = new JMenu("Herramientas");
        JMenuItem findItem = new JMenuItem("Buscar y Reemplazar...");
        findItem.addActionListener(e -> showFindReplaceDialog());
        toolsMenu.add(findItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);
        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    private void createPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem cutPopup = new JMenuItem("Cortar");
        cutPopup.addActionListener(e -> textPane.cut());
        JMenuItem copyPopup = new JMenuItem("Copiar");
        copyPopup.addActionListener(e -> textPane.copy());
        JMenuItem pastePopup = new JMenuItem("Pegar");
        pastePopup.addActionListener(e -> textPane.paste());

        popupMenu.add(cutPopup);
        popupMenu.add(copyPopup);
        popupMenu.add(pastePopup);
        textPane.setComponentPopupMenu(popupMenu);
    }

    private void updateStatus() {
        String text = textPane.getText();
        int chars = text.length();
        String[] words = text.trim().split("\\s+");
        int numWords = text.trim().isEmpty() ? 0 : words.length;
        int lines = textPane.getDocument().getDefaultRootElement().getElementCount();
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
        }
        textPane.setCharacterAttributes(attrs, false);
    }

    private void showFindReplaceDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField findField = new JTextField();
        JTextField replaceField = new JTextField();
        panel.add(new JLabel("Buscar:"));
        panel.add(findField);
        panel.add(new JLabel("Reemplazar:"));
        panel.add(replaceField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Buscar y Reemplazar", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String txt = textPane.getText();
            String find = findField.getText();
            String repl = replaceField.getText();
            if(!find.isEmpty()) {
                textPane.setText(txt.replace(find, repl));
            }
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            // Simulación visual en NUI también
            progressLabel.setState(ProgressLabel.State.WORKING);
            progressLabel.setStatusText("Abriendo...");

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                    textPane.setText("");
                    String line;
                    StyledDocument doc = textPane.getStyledDocument();
                    while ((line = reader.readLine()) != null) {
                        doc.insertString(doc.getLength(), line + "\n", null);
                    }
                    undoManager.discardAllEdits();
                    SwingUtilities.invokeLater(() -> {
                        setTitle("Editor - " + currentFile.getName());
                        progressLabel.setState(ProgressLabel.State.DONE);
                        new Timer(1000, e -> progressLabel.setState(ProgressLabel.State.IDLE)).start();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> progressLabel.setState(ProgressLabel.State.ERROR));
                }
            }).start();
        }
    }

    private void saveFileWithProgress() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    progressLabel.setState(ProgressLabel.State.WORKING);
                    progressLabel.setStatusText("Guardando...");
                });

                String content = textPane.getText();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                    for (int i = 0; i <= 100; i+=5) {
                        Thread.sleep(20); // Simular escritura lenta
                        publish(i);
                    }
                    writer.write(content);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                progressLabel.setProgressValue(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    get();
                    progressLabel.setState(ProgressLabel.State.DONE);
                    setTitle("Editor - " + currentFile.getName());
                    new Timer(2000, e -> progressLabel.setState(ProgressLabel.State.IDLE)).start();
                } catch (Exception e) {
                    progressLabel.setState(ProgressLabel.State.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            if (!currentFile.getName().toLowerCase().endsWith(".txt")) {
                currentFile = new File(currentFile.getAbsolutePath() + ".txt");
            }
            saveFileWithProgress();
        }
    }

    // ===========================================================================
    // TU COMPONENTE PROGRESS LABEL (Mantenido intacto)
    // ===========================================================================
    public static class ProgressLabel extends JPanel {
        public enum State { IDLE, WORKING, DONE, ERROR }
        private JLabel textLabel;
        private JProgressBar progressBar;
        private State currentState;

        public ProgressLabel() {
            super(new FlowLayout(FlowLayout.LEFT, 10, 0));
            textLabel = new JLabel("Listo");
            progressBar = new JProgressBar(0, 100);
            progressBar.setPreferredSize(new Dimension(150, 15));
            progressBar.setVisible(false);
            add(textLabel);
            add(progressBar);
            setState(State.IDLE);
        }

        public void setStatusText(String text) { textLabel.setText(text); }
        public void setProgressValue(int value) { progressBar.setValue(value); }

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
                    textLabel.setForeground(new Color(50, 150, 250)); // Azul
                    break;
                case DONE:
                    progressBar.setVisible(true);
                    progressBar.setValue(100);
                    textLabel.setText("Completado");
                    textLabel.setForeground(new Color(0, 150, 0)); // Verde
                    break;
                case ERROR:
                    progressBar.setVisible(true);
                    textLabel.setText("Error");
                    textLabel.setForeground(Color.RED);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
                // Personalizaciones FlatLaf
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
            } catch (UnsupportedLookAndFeelException ex) {
                System.err.println("FlatLaf no iniciado");
            }
            new EditorTextoGUI().setVisible(true);
        });
    }
}
