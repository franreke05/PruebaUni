package com.franciscor.pruebauni.Login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLogin: (nombre: String, correo: String, contrasena: String) -> Unit,
    onDismiss: () -> Unit
) {
    var popupVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        LoginPopup(
            visible = popupVisible,
            onDismiss = {
                popupVisible = false
                onDismiss()
            },
            onLogin = { nombre, correo, contrasena ->
                onLogin(nombre, correo, contrasena)
                popupVisible = false
            }
        )
    }
}

@Composable
fun LoginPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    onLogin: (nombre: String, correo: String, contrasena: String) -> Unit
) {
    if (!visible) return

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x59000000)),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val minDim = minOf(maxWidth, maxHeight)
            val cardWidth = maxWidth * 0.9f
            val cardHeight = maxHeight * 0.78f
            val titleSize = (minDim.value * 0.07f).sp
            val bodySize = (minDim.value * 0.035f).sp
            val fieldShape = RoundedCornerShape(16.dp)

            Card(
                modifier = Modifier.size(cardWidth, cardHeight),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = minDim * 0.08f, vertical = minDim * 0.07f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(minDim * 0.02f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = minDim * 0.16f, height = minDim * 0.02f)
                                .clip(CircleShape)
                                .background(Color(0x33000000))
                        )
                        Spacer(modifier = Modifier.height(minDim * 0.015f))
                        Text(
                            text = "Registro",
                            style = TextStyle(
                                fontSize = titleSize,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                        )
                        Text(
                            text = "Crea tu cuenta con nombre, correo y contrasena",
                            style = TextStyle(
                                fontSize = bodySize,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(minDim * 0.025f)
                    ) {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            shape = fieldShape,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF2F2F7),
                                focusedContainerColor = Color(0xFFF2F2F7),
                                focusedIndicatorColor = Color(0xFF007AFF),
                                unfocusedIndicatorColor = Color(0x33000000)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = correo,
                            onValueChange = { correo = it },
                            label = { Text("Correo") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = fieldShape,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF2F2F7),
                                focusedContainerColor = Color(0xFFF2F2F7),
                                focusedIndicatorColor = Color(0xFF007AFF),
                                unfocusedIndicatorColor = Color(0x33000000)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contrasena,
                            onValueChange = { contrasena = it },
                            label = { Text("Contrasena") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = fieldShape,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF2F2F7),
                                focusedContainerColor = Color(0xFFF2F2F7),
                                focusedIndicatorColor = Color(0xFF007AFF),
                                unfocusedIndicatorColor = Color(0x33000000)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(minDim * 0.02f)
                    ) {
                        Button(
                            onClick = { onLogin(nombre, correo, contrasena) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(minDim * 0.09f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Registrar",
                                style = TextStyle(fontSize = bodySize, fontWeight = FontWeight.Medium)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = "Cancelar",
                                    style = TextStyle(fontSize = bodySize, color = Color(0xFF007AFF))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
