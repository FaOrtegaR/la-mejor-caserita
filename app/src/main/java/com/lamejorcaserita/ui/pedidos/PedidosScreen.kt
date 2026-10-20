package com.lamejorcaserita.ui.pedidos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamejorcaserita.data.entity.Pedido
import com.lamejorcaserita.ui.theme.DoradoOscuro
import com.lamejorcaserita.ui.theme.MostazaOpaco
import com.lamejorcaserita.ui.theme.MostazaSuave
import com.lamejorcaserita.ui.theme.RojoVino
import com.lamejorcaserita.ui.theme.TextoSecundarioMostaza
import com.lamejorcaserita.ui.theme.TextoSobreMostaza
import com.lamejorcaserita.ui.theme.VerdeHierba
import com.lamejorcaserita.viewmodel.PedidosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    viewModel: PedidosViewModel = viewModel(),
    onNuevoPedido: () -> Unit = {},
    onVerDetalle: (Int) -> Unit = {},
    onIrAProductos: () -> Unit = {}
) {
    val pedidosPendientes by viewModel.pedidosPendientes.collectAsState()
    val pedidosEntregados by viewModel.pedidosEntregados.collectAsState()

    // Limpiar pedidos antiguos al abrir la pantalla
    LaunchedEffect(Unit) {
        viewModel.limpiarPedidosAntiguos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedidos") },
                actions = {
                    IconButton(onClick = onIrAProductos) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Ir a Productos"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevoPedido,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo pedido")
            }
        }
    ) { padding ->
        if (pedidosPendientes.isEmpty() && pedidosEntregados.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No hay pedidos aún",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca el botón + para crear uno",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PENDIENTES
                if (pedidosPendientes.isNotEmpty()) {
                    item {
                        Text(
                            text = "PENDIENTES",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    // Iteramos sobre pedidoConItems
                    items(pedidosPendientes, key = { it.pedido.id }) { pedidoConItems ->
                        PedidoItemCard(
                            pedido = pedidoConItems.pedido,
                            totalCalculado = pedidoConItems.totalCalculado, // <-- AQUÍ pasamos el total recalculado
                            onClick = { onVerDetalle(pedidoConItems.pedido.id) }
                        )
                    }
                }

                // ENTREGADOS
                if (pedidosEntregados.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ENTREGADOS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    // Iteramos sobre pedidoConItems
                    items(pedidosEntregados, key = { it.pedido.id }) { pedidoConItems ->
                        PedidoItemCard(
                            pedido = pedidoConItems.pedido,
                            totalCalculado = pedidoConItems.totalCalculado, // <-- AQUÍ pasamos el total recalculado
                            onClick = { onVerDetalle(pedidoConItems.pedido.id) },
                            esEntregado = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PedidoItemCard(
    pedido: Pedido,
    totalCalculado: Int,
    onClick: () -> Unit,
    esEntregado: Boolean = false
) {
    val formatoFecha = SimpleDateFormat("dd/MM HH:mm", Locale("es", "CL"))
    val fechaTexto = formatoFecha.format(Date(pedido.fecha))

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (esEntregado)
                MostazaSuave.copy(alpha = 0.6f)
            else
                MostazaOpaco
        )
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
                    text = pedido.nombreCliente,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextoSobreMostaza
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Usamos el totalCalculado, no el pedido.total
                    Text(
                        text = "$$totalCalculado",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (esEntregado) TextoSecundarioMostaza else DoradoOscuro,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = if (pedido.pagado) VerdeHierba else RojoVino,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (pedido.pagado) "Pagado" else "Por pagar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fechaTexto,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundarioMostaza
            )
        }
    }
}