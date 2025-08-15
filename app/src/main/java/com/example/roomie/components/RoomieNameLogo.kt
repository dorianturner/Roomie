package com.example.roomie.components

import com.example.roomie.R
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun RoomieNameLogo(
    modifier : Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.roomie_name_logo),
        contentDescription = "Roomie App Name Logo",
        modifier = modifier
    )
}