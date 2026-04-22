package com.zeddihub.mobile.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.theme.StateDanger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    Scaffold(containerColor = colors.background) { padding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.background,
                            colors.primary.copy(alpha = 0.10f),
                            colors.background
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))

                Image(
                    painter = painterResource(R.drawable.logo_banner),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(100.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.register_username_hint)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.register_email_hint)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.register_password_hint)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.passwordConfirm,
                    onValueChange = viewModel::onPasswordConfirmChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.register_password_confirm_hint)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onRememberChange(!state.rememberMe) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = viewModel::onRememberChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primary,
                            uncheckedColor = colors.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(R.string.login_remember),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                val errorText = when (state.errorKind) {
                    RegisterErrorKind.EMPTY -> stringResource(R.string.register_error_empty)
                    RegisterErrorKind.PASSWORD_MISMATCH -> stringResource(R.string.register_error_password_mismatch)
                    RegisterErrorKind.INVALID_USERNAME -> stringResource(R.string.register_error_invalid_username)
                    RegisterErrorKind.INVALID_EMAIL -> stringResource(R.string.register_error_invalid_email)
                    RegisterErrorKind.INVALID_PASSWORD -> stringResource(R.string.register_error_invalid_password)
                    RegisterErrorKind.TAKEN -> stringResource(R.string.register_error_taken)
                    RegisterErrorKind.RATE_LIMITED -> stringResource(R.string.register_error_rate_limited)
                    RegisterErrorKind.NETWORK -> stringResource(R.string.register_error_network)
                    RegisterErrorKind.GENERIC -> stringResource(R.string.register_error_generic)
                    RegisterErrorKind.NONE -> null
                }
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = StateDanger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.register_submit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onBackToLogin) {
                    Text(text = stringResource(R.string.register_have_account))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
