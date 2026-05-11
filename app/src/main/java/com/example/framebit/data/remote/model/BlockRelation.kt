package com.example.framebit.data.remote.model

/**
 * Relación de bloqueo entre el usuario actual y otro, desde el punto de vista
 * del usuario actual.
 *
 *  - [NotBlocked]: ninguno ha bloqueado al otro.
 *  - [IBlockedThem]: el usuario actual ha bloqueado al otro.
 *  - [TheyBlockedMe]: el otro usuario ha bloqueado al actual.
 *
 * En caso de bloqueo mutuo se devuelve [IBlockedThem] (es lo que el usuario
 * "ve" como propio: que es él quien controla la relación).
 */
enum class BlockRelation {
    NotBlocked,
    IBlockedThem,
    TheyBlockedMe
}