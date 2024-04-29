package com.practic.usbterminal.screens.about

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoho.android.usbserial.BuildConfig
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.UsbTerminalScreenAttributes
import com.practic.usbterminal.ui.util.LinkifyText
import java.util.Locale

object AboutScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "About",
)

@Composable
fun AboutScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.about_screen_title) }
    val scrollState = rememberScrollState()

    val timeFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.primary)
            .padding(start = 10.dp, end = 10.dp)
            .verticalScroll(scrollState)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_round),
                contentDescription = "",
                modifier = Modifier
                    .height(120.dp)
                    .width(120.dp)
                    .align(Alignment.TopCenter)
            )
        }

        Row(
            modifier = Modifier
                .padding(top = 20.dp, bottom = 10.dp),
        ) {
            Text(
                text = stringResource(id = R.string.app_name_in_about_screen),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 6.dp),
                fontSize = 22.sp
            )
            Text(
                // BuildConfig.VERSION_NAME is from build.gradle(:app) android/defaultConfig/versionName
                text = "V${BuildConfig.BUILD_TYPE}.${timeFormatter}",
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 12.dp),
                fontSize = 14.sp
            )
        }

        val aboutParagraphs = stringArrayResource(R.array.about_text_in_about_screen)
        aboutParagraphs.forEach {
            LinkifyText(
                text = it,
                color = MaterialTheme.colors.onPrimary,
                linkColor = Color(0xFF6688EE),
                modifier = Modifier
                    .padding(top = 8.dp),
                fontSize = 18.sp
            )
        }

        Text(
            text = stringResource(id = R.string.privacy),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .padding(top = 20.dp),
            fontSize = 22.sp
        )
        val privacyParagraphs = stringArrayResource(R.array.privacy_text_in_about_screen)
        privacyParagraphs.forEach {
            LinkifyText(
                text = it,
                color = MaterialTheme.colors.onPrimary,
                linkColor = Color(0xFF6688EE),
                modifier = Modifier
                    .padding(top = 8.dp),
                fontSize = 18.sp
            )
        }

        Text(
            text = stringResource(id = R.string.third_party),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .padding(top = 20.dp),
            fontSize = 22.sp
        )
        val thirdPartyParagraphs = stringArrayResource(R.array.third_party_text_in_about_screen)
        thirdPartyParagraphs.forEach {
            LinkifyText(
                text = it,
                color = MaterialTheme.colors.onPrimary,
                linkColor = Color(0xFF6688EE),
                modifier = Modifier
                    .padding(top = 8.dp),
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}
