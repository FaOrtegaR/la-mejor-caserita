package com.lamejorcaserita.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamejorcaserita.data.AppDatabase
import com.lamejorcaserita.data.entity.Pedido
import com.lamejorcaserita.data.entity.PedidoConItems
import com.lamejorcaserita.data.entity.PedidoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PedidosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.obtenerBaseDeDatos(application)
    private val pedidoDao = db.pedidoDao()

    // ===== FLUJOS DE PEDIDOS =====

    val pedidosPendientes: StateFlow<List<PedidoConItems>> = pedidoDao.obtenerPendientesConItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pedidosEntregados: StateFlow<List<PedidoConItems>> = pedidoDao.obtenerEntregadosConItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ===== CREAR Y ACTUALIZAR PEDIDOS =====

    fun crearPedido(nombreCliente: String, items: List<PedidoItem>, total: Int) {
        viewModelScope.launch {
            val pedido = Pedido(
                nombreCliente = nombreCliente,
                fecha = System.currentTimeMillis(),
                total = total,
                entregado = false
            )
            pedidoDao.crearPedidoConItems(pedido, items)
        }
    }

    fun agregarProductoAPedido(
        nombreCliente: String,
        productoId: Int,
        nombreProducto: String,
        precioUnitario: Int,
        cantidad: Int
    ) {
        viewModelScope.launch {
            val pedidoExistente = pedidoDao.buscarPedidoPendientePorCliente(nombreCliente)

            if (pedidoExistente != null) {
                pedidoDao.agregarItemAPedido(
                    pedidoId = pedidoExistente.id,
                    productoId = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad = cantidad
                )
            } else {
                val nuevoPedido = Pedido(
                    nombreCliente = nombreCliente,
                    fecha = System.currentTimeMillis(),
                    total = precioUnitario * cantidad,
                    entregado = false
                )
                val pedidoId = pedidoDao.insertarPedido(nuevoPedido)
                val nuevoItem = PedidoItem(
                    pedidoId = pedidoId.toInt(),
                    productoId = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad = cantidad,
                    subtotal = precioUnitario * cantidad
                )
                pedidoDao.insertarItem(nuevoItem)
            }
        }
    }

    fun crearOActualizarPedidoConItems(
        nombreCliente: String,
        items: List<PedidoItem>
    ) {
        viewModelScope.launch {
            val pedidoExistente = pedidoDao.buscarPedidoPendientePorCliente(nombreCliente)

            if (pedidoExistente != null) {
                val totalReal = items.sumOf { it.subtotal }
                pedidoDao.reemplazarItemsDePedido(pedidoExistente.id, items, totalReal)
                pedidoDao.actualizarNombre(pedidoExistente.id, nombreCliente)
            } else {
                val totalReal = items.sumOf { it.subtotal }
                val nuevoPedido = Pedido(
                    nombreCliente = nombreCliente,
                    fecha = System.currentTimeMillis(),
                    total = totalReal,
                    entregado = false,
                    pagado = false
                )
                val pedidoId = pedidoDao.insertarPedido(nuevoPedido)
                val itemsConPedido = items.map { it.copy(pedidoId = pedidoId.toInt()) }
                pedidoDao.insertarItems(itemsConPedido)
            }
        }
    }

    fun actualizarPedido(
        pedidoId: Int,
        nombreCliente: String,
        cantidades: Map<Int, Int>,
        productos: List<com.lamejorcaserita.data.entity.Producto>
    ) {
        viewModelScope.launch {
            val nuevosItems = mutableListOf<PedidoItem>()
            var nuevoTotal = 0

            cantidades.forEach { (productoId, cantidad) ->
                if (cantidad > 0) {
                    val producto = productos.find { it.id == productoId }
                    if (producto != null) {
                        val subtotal = producto.precio * cantidad
                        nuevosItems.add(
                            PedidoItem(
                                pedidoId = pedidoId,
                                productoId = producto.id,
                                nombreProducto = producto.nombre,
                                precioUnitario = producto.precio,
                                cantidad = cantidad,
                                subtotal = subtotal
                            )
                        )
                        nuevoTotal += subtotal
                    }
                }
            }

            pedidoDao.actualizarNombre(pedidoId, nombreCliente)
            pedidoDao.reemplazarItemsDePedido(pedidoId, nuevosItems, nuevoTotal)
        }
    }

    // ===== ACTUALIZAR ESTADOS =====

    fun marcarComoEntregado(pedidoId: Int, entregado: Boolean) {
        viewModelScope.launch {
            pedidoDao.actualizarEstado(pedidoId, entregado)
        }
    }

    fun marcarComoPagado(pedidoId: Int, pagado: Boolean) {
        viewModelScope.launch {
            pedidoDao.actualizarPagado(pedidoId, pagado)
        }
    }

    fun eliminarPedido(pedidoId: Int) {
        viewModelScope.launch {
            pedidoDao.eliminarPedido(pedidoId)
        }
    }

    // ===== OBTENER DATOS =====

    suspend fun obtenerPedidoPorId(pedidoId: Int): Pedido? {
        return pedidoDao.obtenerPorId(pedidoId)
    }

    suspend fun obtenerItemsDePedido(pedidoId: Int): List<PedidoItem> {
        return pedidoDao.obtenerItemsDePedido(pedidoId)
    }

    fun observarItemsDePedido(pedidoId: Int) = pedidoDao.observarItemsDePedido(pedidoId)

    suspend fun recalcularTotalPedido(pedidoId: Int) {
        val items = pedidoDao.obtenerItemsDePedido(pedidoId)
        val nuevoTotal = items.sumOf { it.subtotal }
        pedidoDao.actualizarTotal(pedidoId, nuevoTotal)
    }

    // ===== LIMPIEZA =====

    fun limpiarPedidosAntiguos() {
        viewModelScope.launch {
            val seisDiasEnMillis = 6L * 24 * 60 * 60 * 1000
            val fechaLimite = System.currentTimeMillis() - seisDiasEnMillis
            pedidoDao.eliminarPedidosEntregadosAntiguos(fechaLimite)
        }
    }
}