# La Mejor Caserita 🍽️

App Android nativa para la gestión de pedidos de un negocio familiar de productos caseros.

## 📋 Descripción

Aplicación móvil desarrollada en Kotlin + Jetpack Compose que permite gestionar pedidos de un negocio de comida casera. Incluye reconocimiento de voz para agregar productos, control de estados de pago y entrega, y persistencia local de datos.

**Nota:** Este proyecto fue desarrollado para el negocio familiar "La Mejor Caserita". Todos los datos sensibles han sido anonimizados para esta publicación.

## ✨ Funcionalidades

- **Catálogo de productos:** crear, editar y eliminar productos.
- **Gestión de pedidos:** crear, editar y eliminar pedidos.
- **Reconocimiento de voz:** agregar productos al pedido dictándolos por voz.
- **Detección inteligente de cantidades:** reconoce "tres empanadas", "2 humitas", etc.
- **Múltiples productos por voz:** "dos empanadas y tres humitas".
- **Estados de pedido:** pendiente / entregado.
- **Estados de pago:** por pagar / pagado.
- **Cálculo automático de totales.**
- **Persistencia local** con Room.
- **Limpieza automática** de pedidos antiguos (más de 6 días).
- **Diseño responsive** con paleta de colores personalizada.

## 🛠️ Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Base de datos:** Room (SQLite)
- **Navegación:** Navigation Compose
- **Estado:** ViewModel + StateFlow
- **Voz:** SpeechRecognizer nativo de Android
- **Arquitectura:** MVVM
- **IDE:** Android Studio

## 🎤 Cómo funciona el reconocimiento de voz

1. El usuario toca el botón del micrófono.
2. Dice el pedido: *"dos empanadas de pino y tres humitas"*.
3. La app transcribe el audio con `SpeechRecognizer`.
4. El texto se normaliza (minúsculas, sin tildes, sin puntuación).
5. Se divide en fragmentos por "y", ",".
6. Se buscan coincidencias con los productos del catálogo.
7. Se detectan las cantidades ("dos", "tres", "2", "3").
8. Los productos se agregan al pedido automáticamente.

## 🚀 Cómo ejecutar el proyecto

### Requisitos

- Android Studio Panda 4 o superior
- JDK 17
- Android SDK 34+
- Dispositivo Android (físico o emulador) con API 24+

### Pasos

1. Clona este repositorio:
   ```bash
   git clone https://github.com/FaOrtegaR/la-mejor-caserita.git
