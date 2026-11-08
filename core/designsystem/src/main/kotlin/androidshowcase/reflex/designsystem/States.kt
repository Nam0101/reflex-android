package androidshowcase.reflex.designsystem

import androidshowcase.reflex.domain.error.AppError
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
fun ErrorState(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(errorMessage(error), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Button(onClick = onRetry) { Text(stringResource(R.string.ds_retry)) }
  }
}

@Composable
fun errorMessage(error: AppError): String = when (error) {
  is AppError.Network -> stringResource(R.string.ds_error_network)
  is AppError.Timeout -> stringResource(R.string.ds_error_timeout)
  is AppError.NotFound -> stringResource(R.string.ds_error_not_found)
  is AppError.Server -> stringResource(R.string.ds_error_server, error.statusCode)
  is AppError.Storage -> stringResource(R.string.ds_error_storage)
  is AppError.Unknown -> stringResource(R.string.ds_error_unknown)
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() = ReflexTheme(dynamicColor = false) {
  ErrorState(AppError.Network(), onRetry = {})
}
