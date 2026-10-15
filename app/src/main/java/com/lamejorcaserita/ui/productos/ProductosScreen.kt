package com.lamejorcaserita.ui.productos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamejorcaserita.data.entity.Producto
import com.lamejorcaserita.ui.theme.DoradoOscuro
import com.lamejorcaserita.ui.theme.MostazaOpaco
import com.lamejorcaserita.ui.theme.TextoSobreMostaza
import com.lamejorcaserita.viewmodel.ProductosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(
    viewModel: ProductosViewModel = viewModel(),
    onVolver: () -> Unit = {}
) {
    val productos by viewModel.productos.collectAsState()

    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoAgregar = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        if (productos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay productos aún")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productos, key = { it.id }) { producto ->
                    ProductoItem(
                        producto = producto,
                        onClick = { productoAEditar = producto },
                        onEliminar = { productoAEliminar = producto }
                    )
                }
            }
        }
    }

    // Diálogo para agregar producto
    if (mostrarDialogoAgregar) {
        DialogoProducto(
            titulo = "Agregar producto",
            productoInicial = null,
            onDismiss = { mostrarDialogoAgregar = false },
            onGuardar = { nombre, precio ->
                viewModel.agregarProducto(nombre, precio)
                mostrarDialogoAgregar = false
            }
        )
    }

    // Diálogo para editar producto
    productoAEditar?.let { producto ->
        DialogoProducto(
            titulo = "Editar producto",
            productoInicial = producto,
            onDismiss = { productoAEditar = null },
            onGuardar = { nombre, precio ->
                viewModel.actualizarProducto(
                    producto.copy(nombre = nombre, precio = precio)
                )
                productoAEditar = null
            }
        )
    }

    // Diálogo de confirmación de eliminación
    productoAEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro que quieres eliminar '${producto.nombre}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarProducto(producto)
                        productoAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProductoItem(
    producto: Producto,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoSobreMostaza
                )
                Text(
                    text = "$${producto.precio}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DoradoOscuro
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
fun DialogoProducto(
    titulo: String,
    productoInicial: Producto?,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, precio: Int) -> Unit
) {
    var nombre by remember { mutableStateOf(productoInicial?.nombre ?: "") }
    var precio by remember { mutableStateOf(productoInicial?.precio?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it.filter { c -> c.isDigit() } },
                    label = { Text("Precio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isNotBlank() && precio.isNotBlank()) {
                        onGuardar(nombre.trim(), precio.toInt())
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}