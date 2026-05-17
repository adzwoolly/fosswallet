package nz.eloque.foss_wallet.ui.screens.create

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.OriginalPass
import nz.eloque.foss_wallet.ui.WalletScaffold
import nz.eloque.foss_wallet.ui.screens.pass.PassViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    passId: String,
    navController: NavHostController,
    createViewModel: CreateViewModel,
    passViewModel: PassViewModel,
) {
    val context = LocalContext.current
    val passFlow = passViewModel.passFlowById(passId)
    val localizedPass by passFlow.collectAsState(initial = null)
    val pass = localizedPass ?: return

    val originalPassFile = File(context.filesDir, "$passId/${OriginalPass.FILE_PATH}")
    val isSigned = originalPassFile.exists()

    var warningAccepted by remember { mutableStateOf(!isSigned) }

    if (!warningAccepted) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text(stringResource(R.string.signed_pass_warning_title)) },
            text = { Text(stringResource(R.string.signed_pass_warning_body)) },
            confirmButton = {
                TextButton(onClick = {
                    originalPassFile.delete()
                    warningAccepted = true
                }) {
                    Text(stringResource(R.string.signed_pass_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(stringResource(R.string.back))
                }
            },
        )
        return
    }

    WalletScaffold(
        navController = navController,
        toolWindow = true,
        title = stringResource(R.string.edit_pass),
    ) {
        CreateView(
            navController = navController,
            createViewModel = createViewModel,
            existingPass = pass,
        )
    }
}
