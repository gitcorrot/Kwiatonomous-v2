package com.corrot.kwiatonomousapp.presentation.devices

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corrot.kwiatonomousapp.R
import com.corrot.kwiatonomousapp.common.components.DefaultScaffold
import com.corrot.kwiatonomousapp.common.components.DefaultTopAppBar
import com.corrot.kwiatonomousapp.common.components.ErrorBoxCancel
import com.corrot.kwiatonomousapp.common.components.UserDeviceItem
import com.corrot.kwiatonomousapp.domain.model.DeviceUpdate
import com.corrot.kwiatonomousapp.domain.model.KwiatonomousAppState
import com.corrot.kwiatonomousapp.domain.model.UserDevice
import com.corrot.kwiatonomousapp.presentation.Screen
import com.corrot.kwiatonomousapp.presentation.theme.KwiatonomousAppTheme
import java.time.LocalDateTime

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun DevicesScreen(
    kwiatonomousAppState: KwiatonomousAppState,
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DefaultScaffold(
        scaffoldState = kwiatonomousAppState.scaffoldState,
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.all_devices),
                onNavigateBackClicked = { kwiatonomousAppState.navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { kwiatonomousAppState.navController.navigate(Screen.AddEditUserDevice.route) }
            ) {
                Icon(Icons.Filled.Add, "")
            }
        }
    ) {
        DevicesScreenContent(
            isLoading = state.isLoading,
            error = state.error,
            userDevicesWithLastUpdates = state.userDevicesWithLastUpdates,
            refreshData = viewModel::refreshData,
            confirmError = viewModel::confirmError,
            onUserDeviceClicked = {
                kwiatonomousAppState.navController.navigate(Screen.DeviceDetails.withArgs(it.deviceId))
            }
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun DevicesScreenContent(
    isLoading: Boolean,
    error: String?,
    userDevicesWithLastUpdates: List<Pair<UserDevice, DeviceUpdate?>>?,
    refreshData: () -> Unit,
    confirmError: () -> Unit,
    onUserDeviceClicked: (UserDevice) -> Unit,
) {
    val refreshState = rememberPullRefreshState(isLoading, refreshData)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(refreshState)
        ) {
            userDevicesWithLastUpdates?.let {
                items(it) { userDeviceAndLastUpdate ->
                    UserDeviceItem(
                        userDevice = userDeviceAndLastUpdate.first,
                        lastDeviceUpdate = userDeviceAndLastUpdate.second,
                        onItemClick = onUserDeviceClicked
                    )
                }
            }
        }
        if (!error.isNullOrBlank()) {
            ErrorBoxCancel(
                message = error,
                onCancel = confirmError
            )
        }
        PullRefreshIndicator(
            refreshing = isLoading,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DevicesScreenContentLightPreview() {
    KwiatonomousAppTheme(darkTheme = false) {
        Surface {
            DevicesScreenContent(
                isLoading = true,
                error = null,
                userDevicesWithLastUpdates = listOf(
                    Pair(
                        UserDevice(
                            deviceId = "deviceId",
                            deviceName = "Device 1",
                            deviceImageName = "flower_1",
                            isFavourite = false,
                            notificationsOn = false
                        ),
                        DeviceUpdate(
                            deviceId = "deviceId",
                            updateTime = LocalDateTime.of(2023, 2, 10, 14, 0, 0),
                            batteryLevel = 55,
                            batteryVoltage = 3.6f,
                            temperature = 22.4f,
                            humidity = 77.4f
                        )
                    ),
                    Pair(
                        UserDevice(
                            deviceId = "deviceId2",
                            deviceName = "Device 2",
                            deviceImageName = "flower_2",
                            isFavourite = false,
                            notificationsOn = false
                        ),
                        DeviceUpdate(
                            deviceId = "deviceId2",
                            updateTime = LocalDateTime.of(2023, 2, 10, 14, 0, 0),
                            batteryLevel = 55,
                            batteryVoltage = 3.6f,
                            temperature = 22.4f,
                            humidity = 77.4f
                        )
                    )
                ),
                {}, {}, {}
            )
        }
    }
}
