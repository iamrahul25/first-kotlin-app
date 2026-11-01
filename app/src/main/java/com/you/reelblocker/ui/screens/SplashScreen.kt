package com.you.reelblocker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.you.reelblocker.R
import com.you.reelblocker.ui.components.PrimaryButton
import com.you.reelblocker.ui.theme.ForestDeep
import com.you.reelblocker.ui.theme.Ink
import com.you.reelblocker.ui.theme.Slate

@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_leaf),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
            Text(text = "FocusBlock", color = ForestDeep, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Less scrolling.\nMore living.",
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Image(
            painter = painterResource(R.drawable.img_splash_illustration),
            contentDescription = null,
            modifier = Modifier.size(width = 260.dp, height = 210.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Block short videos. Track your time.\nBuild better habits.",
                color = Slate,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            PrimaryButton(text = "Get Started", onClick = onGetStarted, showArrow = true)
        }

        Spacer(modifier = Modifier.weight(0.5f))
    }
}
