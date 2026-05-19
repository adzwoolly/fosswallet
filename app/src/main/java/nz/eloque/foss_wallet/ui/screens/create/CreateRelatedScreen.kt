package nz.eloque.foss_wallet.ui.screens.create

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.ui.WalletScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRelatedScreen(
    templatePass: LocalizedPassWithTags,
    navController: NavHostController,
    createViewModel: CreateViewModel,
) {
    WalletScaffold(
        navController = navController,
        toolWindow = true,
        title = stringResource(R.string.create_related_pass),
    ) {
        PassEditorView(
            navController = navController,
            createViewModel = createViewModel,
            existingPass = templatePass,
            isTemplate = true,
        )
    }
}
