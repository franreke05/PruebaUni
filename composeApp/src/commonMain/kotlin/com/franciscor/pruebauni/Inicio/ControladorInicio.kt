package com.franciscor.pruebauni.Inicio


// Controlador de Inicio (orquestacion de Firebase).
/*
Implementacion Firebase (sin peer-to-peer):
1) Identidad: usa Firebase Auth anonimo para dar un uid estable a cada jugador.
2) Modelo de datos en Firestore:
   - rooms/{roomId}: estado global (turnoActual, cartaActual, mazo, descarte, estadoPartida).
   - rooms/{roomId}/players/{uid}: nombre, mano, estado, ultimoHeartbeat.
3) Flujos:
   - crearSala(): crea room y pone al host en players.
   - unirseSala(codigo): busca room por codigo y agrega player.
   - jugarCarta/robar: escribe en room + player con transaccion para evitar condiciones de carrera.
4) Validacion:
   - Si quieres mas seguridad, mueve barajar/repartir/validar a Cloud Functions.
   - Si confias en amigos, el host puede validar localmente y escribir resultados (host-authoritative).
5) Presencia:
   - Usa un campo lastSeen con serverTimestamp y limpia jugadores inactivos.
*/

