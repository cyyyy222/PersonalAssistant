package com.example.personalassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.personalassistant.data.AssistantState
import com.example.personalassistant.ui.theme.*

@Composable
fun LoginScreen(state: AssistantState, onLogin: () -> Unit) {
    val focus = LocalFocusManager.current
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = SoftGray,
        focusedContainerColor = SoftGray,
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = Green,
        cursorColor = Green
    )

    Column(
        Modifier.fillMaxSize().background(Canvas).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppIcon(Glyph.Spark, tint = Green)
                Text("个人助理", style = MaterialTheme.typography.titleMedium)
            }

            Text("登录", style = MaterialTheme.typography.headlineLarge)

            SegmentedControl(listOf("账号", "手机号"), state.loginMode, onSelect = {
                state.loginMode = it
                state.loginError = null
            })

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = state.loginAccount,
                    onValueChange = {
                        state.loginAccount = it
                        state.loginError = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("login_account"),
                    singleLine = true,
                    placeholder = { Text(if (state.loginMode == "手机号") "手机号" else "用户名") },
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (state.loginMode == "手机号") KeyboardType.Phone else KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = state.loginPassword,
                    onValueChange = {
                        state.loginPassword = it
                        state.loginError = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("login_password"),
                    singleLine = true,
                    placeholder = { Text("密码") },
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = fieldColors,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus()
                        onLogin()
                    }),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            AppIcon(if (showPassword) Glyph.EyeOff else Glyph.Eye, Modifier.size(20.dp),
                                tint = Muted, description = if (showPassword) "隐藏密码" else "显示密码")
                        }
                    }
                )
            }

            state.loginError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    focus.clearFocus()
                    onLogin()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("login_button"),
                enabled = !state.loginLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.loginLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("登录中")
                } else {
                    Text("登录")
                }
            }
        }
    }
}
