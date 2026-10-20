package com.lamejorcaserita.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PedidoConItems(
    @Embedded val pedido: Pedido,
    @Relation(
        parentColumn = "id",
        entityColumn = "pedidoId"
    )
    val items: List<PedidoItem>
) {
    // Calculamos el total real sumando los subtotales de los items actuales
    val totalCalculado: Int
        get() = items.sumOf { it.subtotal }
}