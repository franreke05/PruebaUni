package com.franciscor.pruebauni.Partida

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun Tablero() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F1EC))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        val outerPad = minDim * 0.035f
        val cardWidth = minDim * 0.18f
        val cardHeight = cardWidth * 1.45f
        val smallCardWidth = cardWidth * 0.58f
        val smallCardHeight = smallCardWidth * 1.45f
        val cardCorner = cardWidth * 0.12f
        val smallCorner = smallCardWidth * 0.12f
        val border = minDim * 0.006f
        val textSmall = (minDim.value * 0.03f).sp
        val textLarge = (minDim.value * 0.065f).sp
        val pileGap = minDim * 0.07f
        val stackOffset = minDim * 0.006f

        val red = Color(0xFFE53935)
        val yellow = Color(0xFFFBC02D)
        val green = Color(0xFF43A047)
        val blue = Color(0xFF1E88E5)
        val black = Color(0xFF151515)
        val tableDark = Color(0xFF0E3B2B)
        val tableLight = Color(0xFF1C5F47)
        val ink = Color(0xFF0C0C0C)

        val tableShape = RoundedCornerShape(minDim * 0.08f)
        val cardShape = RoundedCornerShape(cardCorner)
        val smallCardShape = RoundedCornerShape(smallCorner)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(tableShape)
                    .background(Brush.verticalGradient(listOf(tableLight, tableDark)))
                    .border(border, Color(0xFF0A2A1F), tableShape)
            )

            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "UNO",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = textLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (minDim.value * 0.004f).sp
                    )
                )
                Spacer(Modifier.height(minDim * 0.01f))
                Row(
                    modifier = Modifier
                        .background(Color(0x66000000), RoundedCornerShape(50))
                        .padding(
                            horizontal = minDim * 0.03f,
                            vertical = minDim * 0.01f
                        ),
                    horizontalArrangement = Arrangement.spacedBy(minDim * 0.02f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Ronda 1",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Box(
                        Modifier
                            .size(minDim * 0.01f)
                            .background(Color.White, CircleShape)
                    )
                    BasicText(
                        "Tu turno",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = minDim * 0.16f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "J2 · 5 cartas",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                Spacer(Modifier.height(minDim * 0.01f))
                Row(horizontalArrangement = Arrangement.spacedBy(minDim * 0.008f)) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .size(smallCardWidth, smallCardHeight)
                                .clip(smallCardShape)
                                .background(black)
                                .border(minDim * 0.002f, Color(0xFF222222), smallCardShape)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = minDim * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(minDim * 0.008f)
            ) {
                BasicText(
                    "J3 · 4",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(smallCardWidth, smallCardHeight)
                            .clip(smallCardShape)
                            .background(black)
                            .border(minDim * 0.002f, Color(0xFF222222), smallCardShape)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = minDim * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(minDim * 0.008f)
            ) {
                BasicText(
                    "J4 · 3",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(smallCardWidth, smallCardHeight)
                            .clip(smallCardShape)
                            .background(black)
                            .border(minDim * 0.002f, Color(0xFF222222), smallCardShape)
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(pileGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(cardWidth, cardHeight)) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .matchParentSize()
                                .offset(
                                    x = stackOffset * i.toFloat(),
                                    y = -stackOffset * i.toFloat()
                                )
                                .clip(cardShape)
                                .background(black)
                                .border(minDim * 0.003f, Color(0xFF222222), cardShape)
                        )
                    }
                    BasicText(
                        "ROBAR",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(Modifier.size(cardWidth, cardHeight)) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(cardShape)
                            .background(black)
                            .border(minDim * 0.003f, Color(0xFF222222), cardShape)
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .padding(cardWidth * 0.08f)
                            .clip(cardShape)
                            .background(Color(0xFF101010))
                            .rotate(-12f)
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxHeight().background(red))
                            Box(Modifier.weight(1f).fillMaxHeight().background(yellow))
                            Box(Modifier.weight(1f).fillMaxHeight().background(green))
                            Box(Modifier.weight(1f).fillMaxHeight().background(blue))
                        }
                    }
                    BasicText(
                        "7",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = textLarge,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = minDim * 0.02f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "Tu mano",
                    style = TextStyle(color = Color.White, fontSize = textSmall)
                )
                Spacer(Modifier.height(minDim * 0.01f))
                Row(horizontalArrangement = Arrangement.spacedBy(minDim * 0.01f)) {
                    val handColors = listOf(red, yellow, green, blue, red, green, black)
                    val angles = listOf(-10f, -6f, -3f, 0f, 3f, 6f, 10f)
                    handColors.forEachIndexed { index, color ->
                        Box(
                            Modifier
                                .size(cardWidth * 0.9f, cardHeight * 0.9f)
                                .rotate(angles[index])
                                .clip(cardShape)
                                .background(Color.White)
                                .border(minDim * 0.003f, ink, cardShape)
                        ) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .padding(cardWidth * 0.08f)
                                    .clip(cardShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
