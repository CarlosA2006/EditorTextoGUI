# UT3RA4: Evaluación y Propuestas de Mejora de Usabilidad y Accesibilidad

## 1. Evaluación de la Interfaz (Fase 1)

### Heurísticas de Nielsen (Problemas Detectados)

**1. Visibilidad del estado del sistema**
*   **Problema:** Al guardar o abrir archivos, si ocurre un error grave, el sistema solo cambia el texto de una etiqueta pequeña a "Error", que puede pasar desapercibido.
*   **Impacto:** El usuario puede creer que su trabajo se ha guardado cuando no es así.

**2. Ayuda y documentación**
*   **Problema:** El sistema NUI (comandos de voz) es una característica compleja, pero no hay una lista accesible de qué comandos exactos entiende el sistema. Solo hay un texto de ejemplo muy pequeño.
*   **Impacto:** El usuario tiene que adivinar las palabras clave ("¿será 'guardar' o 'salvar'?"), generando frustración.

**3. Reconocimiento antes que recuerdo**
*   **Problema:** Los usuarios deben recordar los comandos de voz. No hay ayudas visuales o autocompletado en el campo de "Comando de voz".
*   **Impacto:** Aumenta la carga cognitiva del usuario.

**4. Prevención de errores**
*   **Problema:** El campo de "Comando de voz" permite escribir cualquier texto y enviarlo, incluso si no es un comando válido, resultando en un mensaje de consola que el usuario normal no ve.
*   **Impacto:** El usuario pierde tiempo escribiendo comandos que el sistema no va a procesar.

**5. Estética y diseño minimalista**
*   **Problema:** La interfaz mezcla estilos (bordes por defecto, colores hardcoded en el panel NUI) sin una coherencia visual clara con el tema oscuro (FlatLaf).
*   **Impacto:** Reduce la percepción de calidad del producto.

### Accesibilidad (WCAG 2.2)

**1. Operable por teclado (2.1)**
*   **Problema:** El botón de "Simular" y el campo de texto no tienen atajos de teclado o mnemónicos claros definidos explícitamente en la interfaz visual (tooltips).

**2. Información y relaciones (1.3.1)**
*   **Problema:** Los campos de formulario (como el de comando de voz) tienen etiquetas visuales cercanas, pero no están programáticamente asociados (falta de tooltips descriptivos en el componente mismo).

**3. Ayuda a la entrada de datos (3.3)**
*   **Problema:** No se proporcionan sugerencias de corrección si el usuario escribe mal un comando de voz.

---

## 2. Propuestas de Mejora (Fase 2)

He seleccionado los siguientes problemas para solucionar en esta iteración:

**Propuesta 1: Sistema de Ayuda para Comandos (Resuelve Nielsen #2 y #3)**
*   **Solución:** Implementar un botón de ayuda [?] visible junto al panel de comandos. Al pulsarlo, mostrará una ventana con la lista completa de comandos válidos.
*   **Justificación:** Permite al usuario reconocer los comandos disponibles en lugar de tener que memorizarlos o adivinarlos.

**Propuesta 2: Feedback de Error Robusto (Resuelve Nielsen #1)**
*   **Solución:** Implementar ventanas emergentes (Modal Dialogs) críticas cuando falla la operación de guardar/abrir.
*   **Justificación:** Un error de guardado es crítico; el estado del sistema debe ser visible de forma imperativa para evitar pérdida de datos.

**Propuesta 3: Accesibilidad mejorada (Resuelve WCAG 2.1 y 1.3.1)**
*   **Solución:** Añadir `Tooltips` descriptivos a todos los elementos interactivos y asegurarse de que los botones tengan mnemónicos (Atajos Alt+Letra).
*   **Justificación:** Facilita el uso a personas que dependen de lectores de pantalla o navegación por teclado.

---

## 3. Implementación y Reevaluación (Fase 3 y 4)

**Cambios realizados:**
1.  Se ha añadido un botón **[?]** en el panel NUI que despliega una lista clara de comandos.
2.  Se han implementado **Tooltips** en los campos de entrada y botones para mejorar la accesibilidad y el descubrimiento de funciones.
3.  Se han añadido **Diálogos Modales de Error** (`JOptionPane`) en las funciones de abrir y guardar archivo.

**Reevaluación de Heurísticas:**
*   **Visibilidad del estado:** Ahora, si ocurre un error, el sistema detiene el flujo y avisa al usuario explícitamente. Se cumple mejor la heurística #1.
*   **Ayuda y Documentación:** La heurística #10 (Ayuda) que antes no se cumplía en absoluto para la parte de voz, ahora tiene una implementación funcional y accesible.
*   **Accesibilidad:** Los tooltips permiten que un lector de pantalla anuncie "¿Para qué sirve esto?" al poner el foco en el campo de texto, cumpliendo mejor con WCAG 3.3 (Ayuda a la entrada).

**Conclusión:**
Las mejoras han aumentado significativamente la usabilidad del sistema de voz, transformándolo de una "caja negra" a una herramienta con documentación integrada. La robustez ante errores también ha mejorado la confianza del usuario (Safety).
