package com.lamejorcaserita.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombreCliente: String,
    val fecha: Long,
    val total: Int,
    val entregado: Boolean = false,
    val pagado: Boolean = false
)