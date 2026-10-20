package com.lamejorcaserita.ui.nuevopedido

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamejorcaserita.data.entity.Producto
import com.lamejorcaserita.viewmodel.PedidosViewModel
import com.lamejorcaserita.viewmodel.ProductosViewModel
import com.lamejorcaserita.ui.theme.DoradoOscuro
import com.lamejorcaserita.ui.theme.MostazaOpaco
import com.lamejorcaserita.ui.theme.TextoSobreMostaza
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import com.lamejorcaserita.data.entity.PedidoItem
import com.lamejorcaserita.voz.ProcesadorDeVoz
import com.lamejorcaserita.voz.ReconocimientoVoz

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoPedidoScreen(
    pedidoId: Int? = null,
    onVolver: () -> Unit = {},
    onPedidoGuardado: () -> Unit = {},
    pedidosViewModel: PedidosViewModel = viewModel(),
    productosViewModel: ProductosViewModel = viewModel()
) {
    val productos by productosViewModel.productos.collectAsState()

    // Iniciamos "cargando" en true. Solo se apagará cuando los datos estén listos.
    var cargando by remember { mutableStateOf(true) }
    var nombreCliente by remember { mutableStateOf("") }
    val cantidades = remember { mutableStateMapOf<Int, Int>() }

    // El total se calcula siempre basándose en el estado actual
    val total = productos.sumOf { producto ->
        val cantidad = cantidades[producto.id] ?: 0
        producto.precio * cantidad
    }

    // 1. CARGA INICIAL (Espera a que los productos y el pedido estén listos)
    LaunchedEffect(pedidoId, productos) {
        // Si estamos editando, esperamos a que los productos carguen antes de traer los items
        if (pedidoId != null && productos.isNotEmpty() && cantidades.isEmpty()) {
            val pedido = pedidosViewModel.obtenerPedidoPorId(pedidoId)
            val items = pedidosViewModel.obtenerItemsDePedido(pedidoId)
            if (pedido != null) {
                nombreCliente = pedido.nombreCliente
                cantidades.clear() // Limpiamos por si acaso
                items.forEach { item ->
                    cantidades[item.productoId] = item.cantidad
                }
            }
        }

        // Si es un pedido nuevo, o si ya cargaron los productos en edición, apagamos el cargando
        if (productos.isNotEmpty() || pedidoId == null) {
            cargando = false
        }
    }

    // ===== ESTADO PARA EL RECONOCIMIENTO DE VOZ =====
    val context = LocalContext.current
    val reconocimientoVoz = remember { ReconocimientoVoz(context) }
    val estadoVoz by reconocimientoVoz.estado.collectAsState()
    val textoVoz by reconocimientoVoz.textoReconocido.collectAsState()
    val errorVoz by reconocimientoVoz.error.collectAsState()

    // 2. LIBERAR RECURSOS (Evita el crash)
    DisposableEffect(Unit) {
        onDispose {
            reconocimientoVoz.cancelar() //
            reconocimientoVoz.liberar()
        }
    }

    // Permiso de micrófono
    val launcherPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            reconocimientoVoz.iniciarEscucha()
        }
    }

    val onMicrofonoClick = {
        when (estadoVoz) {
            ReconocimientoVoz.Estado.ESCUCHANDO -> reconocimientoVoz.cancelar()
            ReconocimientoVoz.Estado.PROCESANDO -> Unit
            else -> launcherPermiso.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // PROCESAR RESULTADO DEL RECONOCIMIENTO
    LaunchedEffect(textoVoz) {
        textoVoz?.let { texto ->
            val resultado = ProcesadorDeVoz.procesar(texto, productos)

            if (resultado.itemsDetectados.isEmpty()) {
                android.widget.Toast.makeText(context, "No reconocí ningún producto.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                val nuevosCambios = mutableMapOf<Int, Int>()
                val nombres = mutableListOf<String>()

                resultado.itemsDetectados.forEach { item ->
                    val actualEnPantalla = cantidades[item.producto.id] ?: 0
                    val actualEnLote = nuevosCambios[item.producto.id] ?: 0
                    val cantidadFinal = actualEnPantalla + actualEnLote + item.cantidad
                    nuevosCambios[item.producto.id] = cantidadFinal - actualEnPantalla

                    if (item.producto.id !in cantidades.keys || item.cantidad > 0) {
                        nombres.add("${item.cantidad}x ${item.producto.nombre}")
                    }
                }

                nuevosCambios.forEach { (productoId, cantidadAdicional) ->
                    val actual = cantidades[productoId] ?: 0
                    cantidades[productoId] = actual + cantidadAdicional
                }

                android.widget.Toast.makeText(context, "Agregado: ${nombres.joinToString(", ")}", android.widget.Toast.LENGTH_LONG).show()
            }
            reconocimientoVoz.reiniciar()
        }
    }

    LaunchedEffect(errorVoz) {
        errorVoz?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            reconocimientoVoz.reiniciar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pedidoId != null) "Editar pedido" else "Nuevo pedido") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total:", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "$$total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. BOTÓN DE GUARDAR CORREGIDO
                    Button(
                        onClick = {
                            if (nombreCliente.isNotBlank() && cantidades.values.any { it > 0 }) {
                                if (pedidoId != null) {
                                    // EDITANDO: actualizar el pedido existente
                                    pedidosViewModel.actualizarPedido(
                                        pedidoId = pedidoId,
                                        nombreCliente = nombreCliente.trim(),
                                        cantidades = cantidades.toMap(),
                                        productos = productos
                                    )
                                } else {
                                    // Crear UN SOLO pedido con todos los items
                                    val items = cantidades
                                        .filter { it.value > 0 }
                                        .mapNotNull { (prodId, cantidad) ->
                                            val producto = productos.find { it.id == prodId }
                                            if (producto != null) {
                                                PedidoItem(
                                                    pedidoId = 0,
                                                    productoId = producto.id,
                                                    nombreProducto = producto.nombre,
                                                    precioUnitario = producto.precio,
                                                    cantidad = cantidad, // La cantidad real
                                                    subtotal = producto.precio * cantidad // El subtotal real
                                                )
                                            } else null
                                        }

                                    pedidosViewModel.crearOActualizarPedidoConItems(
                                        nombreCliente = nombreCliente.trim(),
                                        items = items
                                    )
                                }
                                onPedidoGuardado()
                            }
                        },
                        // Se habilita si hay nombre Y al menos 1 producto
                        enabled = nombreCliente.isNotBlank() && cantidades.values.any { it > 0 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pedidoId != null) "Guardar cambios" else "Guardar pedido")
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onMicrofonoClick,
                shape = CircleShape,
                containerColor = when (estadoVoz) {
                    ReconocimientoVoz.Estado.ESCUCHANDO -> MaterialTheme.colorScheme.error
                    ReconocimientoVoz.Estado.PROCESANDO -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .size(96.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Agregar por voz", modifier = Modifier.size(48.dp), tint = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        // 4. MOSTRAR PANTALLA DE CARGA
        if (cargando) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = nombreCliente,
                    onValueChange = { nombreCliente = it },
                    label = { Text("Nombre del cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                Divider()

                if (productos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay productos. Agrega uno primero.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productos, key = { it.id }) { producto ->
                            ProductoSelectorCard(
                                producto = producto,
                                cantidad = cantidades[producto.id] ?: 0,
                                onAgregar = { cantidades[producto.id] = (cantidades[producto.id] ?: 0) + 1 },
                                onQuitar = {
                                    val actual = cantidades[producto.id] ?: 0
                                    if (actual > 1) {
                                        cantidades[producto.id] = actual - 1
                                    } else {
                                        cantidades.remove(producto.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductoSelectorCard(
    producto: Producto,
    cantidad: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MostazaOpaco)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = producto.nombre, style = MaterialTheme.typography.titleMedium, color = TextoSobreMostaza)
                Text(text = "$${producto.precio}", style = MaterialTheme.typography.bodyMedium, color = DoradoOscuro)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cantidad > 0) {
                    IconButton(onClick = onQuitar) {
                        Icon(Icons.Default.Remove, contentDescription = "Quitar")
                    }
                    Text(
                        text = cantidad.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                IconButton(onClick = onAgregar) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        }
    }
}