package com.franciscor.pruebauni.Inicio


// Controlador de Inicio (orquestacion de Firebase).
/*
Implementacion minima (solo identidad):
1) Identidad: Firebase Auth anonimo -> uid estable.
2) Registro: users/{uid} con name, createdAt y lastSeen.
3) Sala opcional: rooms/{roomId} con seats {A, B, C, D} y code.
4) No guardes estado del juego en BD; solo identificacion.
*/

