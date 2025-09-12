package com.example.roomie.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.roomie.R

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat)
)

val NunitoSansFontFamily = FontFamily(
    Font(R.font.nunito_sans, weight = FontWeight.W400), // Regular
    Font(R.font.nunito_sans, weight = FontWeight.W500), // Medium
    Font(R.font.nunito_sans, weight = FontWeight.W600), // SemiBold
    Font(R.font.nunito_sans, weight = FontWeight.W700), // Bold
)