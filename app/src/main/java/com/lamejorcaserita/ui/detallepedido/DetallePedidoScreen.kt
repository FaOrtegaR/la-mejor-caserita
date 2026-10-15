package com.lamejorcaserita.ui.detallepedido

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamejorcaserita.data.entity.Pedido
import com.lamejorcaserita.data.entity.PedidoItem
import com.lamejorcaserita.ui.theme.DoradoOscuro
import com.lamejorcaserita.ui.theme.MostazaOpaco
import com.lamejorcaserita.ui.theme.RojoVino
import com.lamejorcaserita.ui.theme.TextoSecundarioMostaza
import com.lamejorcaserita.ui.theme.TextoSobreMostaza
import com.lamejorcaserita.ui.theme.VerdeHierba
import com.lamejorcaserita.ui.theme.VerdeSombra
import com.lamejorcaserita.viewmodel.PedidosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePedidoScreen(
    pedidoId: Int,
    onVolver: () -> Unit = {},
    onEditar: (Int) -> Unit = {},
    viewModel: PedidosViewModel = viewModel()
) {
    var pedido by remember { mutableStateOf<Pedido?>(null) }
    var items by remember { mutableStateOf<List<PedidoItem>>(emptyList()) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var recargar by remember { mutableStateOf(0) }

    // Cargar pedido y sus items
    LaunchedEffect(pedidoId, recargar) {
        pedido = viewModel.obtenerPedidoPorId(pedidoId)
        items = viewModel.obtenerItemsDePedido(pedidoId)
    }

    val pedidoActual = pedido

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pedidoActual?.nombreCliente ?: "Detalle del pedido") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoEliminar = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar pedido")
                    }
                }
            )
        }
    ) { padding ->
        if (pedidoActual == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // INFO DEL PEDIDO
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MostazaOpaco
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))
                        Text(
                            text = "Fecha: ${formatoFecha.format(Date(pedidoActual.fecha))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundarioMostaza
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Estado: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundarioMostaza
                            )
                            Text(
                                text = if (pedidoActual.entregado) "Entregado" else "Pendiente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (pedidoActual.entregado) VerdeSombra else DoradoOscuro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pago: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundarioMostaza
                            )
                            Surface(
                                color = if (pedidoActual.pagado) VerdeHierba else RojoVino,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (pedidoActual.pagado) "Pagado" else "Por pagar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // LISTA DE ITEMS
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "PRODUCTOS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(items, key = { it.id }) { item ->
                        ItemPedidoCard(item = item)
                    }
                }

                // TOTAL Y BOTONES
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "$${pedidoActual.total}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!pedidoActual.pagado) {
                            Button(
                                onClick = {
                                    viewModel.marcarComoPagado(pedidoActual.id, true)
                                    recargar++  // Para recargar el detalle
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("Marcar como pagado")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.marcarComoPagado(pedidoActual.id, false)
                                    recargar++
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Marcar como por pagar")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!pedidoActual.entregado) {
                            OutlinedButton(
                                onClick = { onEditar(pedidoActual.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Editar pedido")
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.marcarComoEntregado(pedidoActual.id, true)
                                    onVolver()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Marcar como entregado")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.marcarComoEntregado(pedidoActual.id, false)
                                    onVolver()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Marcar como pendiente")
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO DE ELIMINAR
    if (mostrarDialogoEliminar && pedidoActual != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar pedido") },
            text = { Text("¿Estás seguro que quieres eliminar este pedido? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarPedido(pedidoActual.id)
                        mostrarDialogoEliminar = false
                        onVolver()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ItemPedidoCard(item: PedidoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MostazaOpaco
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombreProducto,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoSobreMostaza
                )
                Text(
                    text = "${item.cantidad} x $${item.precioUnitario}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundarioMostaza
                )
            }
            Text(
                text = "$${item.subtotal}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextoSobreMostaza
            )
        }
    }
}