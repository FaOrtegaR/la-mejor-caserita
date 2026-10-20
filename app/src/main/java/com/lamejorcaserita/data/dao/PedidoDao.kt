package com.lamejorcaserita.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.lamejorcaserita.data.entity.Pedido
import com.lamejorcaserita.data.entity.PedidoConItems
import com.lamejorcaserita.data.entity.PedidoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {

    // ===== PEDIDOS =====

    @Transaction
    @Query("SELECT * FROM pedidos WHERE entregado = 0 ORDER BY fecha DESC")
    fun obtenerPendientesConItems(): Flow<List<PedidoConItems>>

    @Transaction
    @Query("SELECT * FROM pedidos WHERE entregado = 1 ORDER BY fecha DESC")
    fun obtenerEntregadosConItems(): Flow<List<PedidoConItems>>

    @Query("SELECT * FROM pedidos WHERE entregado = 0 ORDER BY fecha DESC")
    fun obtenerPendientes(): Flow<List<Pedido>>

    @Query("SELECT * FROM pedidos WHERE entregado = 1 ORDER BY fecha DESC")
    fun obtenerEntregados(): Flow<List<Pedido>>

    @Query("SELECT * FROM pedidos WHERE id = :pedidoId")
    suspend fun obtenerPorId(pedidoId: Int): Pedido?

    @Query("""
        SELECT * FROM pedidos 
        WHERE entregado = 0 AND LOWER(nombreCliente) = LOWER(:nombreCliente) 
        LIMIT 1
    """)
    suspend fun buscarPedidoPendientePorCliente(nombreCliente: String): Pedido?

    @Insert
    suspend fun insertarPedido(pedido: Pedido): Long

    @Query("UPDATE pedidos SET entregado = :entregado WHERE id = :id")
    suspend fun actualizarEstado(id: Int, entregado: Boolean)

    @Query("UPDATE pedidos SET total = :total WHERE id = :id")
    suspend fun actualizarTotal(id: Int, total: Int)

    @Query("DELETE FROM pedidos WHERE id = :id")
    suspend fun eliminarPedido(id: Int)

    @Query("DELETE FROM pedidos WHERE entregado = 1 AND fecha < :fechaLimite")
    suspend fun eliminarPedidosEntregadosAntiguos(fechaLimite: Long)

    @Query("SELECT * FROM pedidos ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<Pedido>>

    // ===== ITEMS =====

    @Query("SELECT * FROM pedido_items WHERE pedidoId = :pedidoId")
    suspend fun obtenerItemsDePedido(pedidoId: Int): List<PedidoItem>

    @Query("SELECT * FROM pedido_items WHERE pedidoId = :pedidoId")
    fun observarItemsDePedido(pedidoId: Int): Flow<List<PedidoItem>>

    @Insert
    suspend fun insertarItems(items: List<PedidoItem>)

    @Insert
    suspend fun insertarItem(item: PedidoItem): Long

    @Query("""
        SELECT * FROM pedido_items 
        WHERE pedidoId = :pedidoId AND productoId = :productoId 
        LIMIT 1
    """)
    suspend fun buscarItemEnPedido(pedidoId: Int, productoId: Int): PedidoItem?

    @Query("""
        UPDATE pedido_items 
        SET cantidad = :cantidad, subtotal = :subtotal 
        WHERE id = :itemId
    """)
    suspend fun actualizarItem(itemId: Int, cantidad: Int, subtotal: Int)

    @Query("DELETE FROM pedido_items WHERE id = :itemId")
    suspend fun eliminarItem(itemId: Int)

    @Query("DELETE FROM pedido_items WHERE pedidoId = :pedidoId")
    suspend fun eliminarItemsDePedido(pedidoId: Int)

    @Query("UPDATE pedidos SET nombreCliente = :nombre WHERE id = :pedidoId")
    suspend fun actualizarNombre(pedidoId: Int, nombre: String)

    @Query("UPDATE pedidos SET pagado = :pagado WHERE id = :id")
    suspend fun actualizarPagado(id: Int, pagado: Boolean)

    // ===== TRANSACCIONES =====

    @Transaction
    suspend fun reemplazarItemsDePedido(pedidoId: Int, nuevosItems: List<PedidoItem>, nuevoTotal: Int) {
        // 1. Borramos todos los items viejos de este pedido
        eliminarItemsDePedido(pedidoId)

        // 2. Insertamos la lista exacta de items nuevos
        val itemsConPedido = nuevosItems.map { it.copy(pedidoId = pedidoId) }
        insertarItems(itemsConPedido)

        // 3. Actualizamos el total en la tabla pedidos
        actualizarTotal(pedidoId, nuevoTotal)
    }

    @Transaction
    suspend fun crearPedidoConItems(pedido: Pedido, items: List<PedidoItem>): Long {
        val pedidoId = insertarPedido(pedido)
        val itemsConPedido = items.map { it.copy(pedidoId = pedidoId.toInt()) }
        insertarItems(itemsConPedido)
        return pedidoId
    }

    @Transaction
    suspend fun agregarItemAPedido(pedidoId: Int, productoId: Int, nombreProducto: String, precioUnitario: Int, cantidad: Int): Int {
        val itemExistente = buscarItemEnPedido(pedidoId, productoId)
        if (itemExistente != null) {
            val nuevaCantidad = itemExistente.cantidad + cantidad
            val nuevoSubtotal = precioUnitario * nuevaCantidad
            actualizarItem(itemExistente.id, nuevaCantidad, nuevoSubtotal)
        } else {
            val nuevoItem = PedidoItem(
                pedidoId = pedidoId,
                productoId = productoId,
                nombreProducto = nombreProducto,
                precioUnitario = precioUnitario,
                cantidad = cantidad,
                subtotal = precioUnitario * cantidad
            )
            insertarItem(nuevoItem)
        }

        // Recalcular el total sumando los subtotales reales de la BD
        val itemsActuales = obtenerItemsDePedido(pedidoId)
        val nuevoTotal = itemsActuales.sumOf { it.subtotal }
        actualizarTotal(pedidoId, nuevoTotal)

        return nuevoTotal
    }
}