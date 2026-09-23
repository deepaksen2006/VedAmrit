package com.example.vedaahar.agentic

import android.app.Application
import com.example.vedaahar.auth.AuthRepository
import com.example.vedaahar.auth.AuthUiState
import com.example.vedaahar.auth.AuthViewModel
import com.example.vedaahar.PatientProfileViewModel
import com.example.vedaahar.network.AuthApi
import com.example.vedaahar.network.TokenStorage
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.FinalReportResponse
import com.example.vedaahar.network.dto.TokenRefreshRequest
import com.example.vedaahar.network.dto.TokenRefreshResponse
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.network.dto.WellnessPlanStatusResponse
import com.example.vedaahar.profile.PatientProfileGateway
import com.example.vedaahar.profile.PatientProfileLoadResult
import com.example.vedaahar.profile.StoredPatientProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class ViewModelFactoryTest {
    @Test
    fun authFactoryCreatesViewModelWithProvidedRepository() {
        val repository = AuthRepository(FakeAuthApi(), FakeTokenStore())

        val viewModel = AuthViewModel.Factory(Application(), repository)
            .create(AuthViewModel::class.java)

        assertEquals(AuthViewModel::class.java, viewModel::class.java)
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun agenticFactoryCreatesViewModelWithProvidedGateway() {
        val viewModel = AgenticPlanViewModel.Factory(
            application = Application(),
            repository = FakeAgenticPlanGateway(),
            latestPlanIdProvider = { null }
        ).create(AgenticPlanViewModel::class.java)

        assertEquals(AgenticPlanViewModel::class.java, viewModel::class.java)
        assertEquals(AgenticPlanUiState.Idle, viewModel.uiState.value)
        assertNull(viewModel.latestPlanId.value)
    }

    @Test
    fun patientProfileFactoryCreatesViewModelWithProvidedRepository() {
        val viewModel = PatientProfileViewModel.Factory(FakePatientProfileGateway())
            .create(PatientProfileViewModel::class.java)

        assertEquals(PatientProfileViewModel::class.java, viewModel::class.java)
        assertEquals("", viewModel.uiState.value.fullName)
    }

    private class FakeAuthApi : AuthApi {
        override suspend fun register(request: AuthRequest): Response<AuthResponse> {
            return Response.success(AuthResponse("access", "refresh", null))
        }

        override suspend fun login(request: AuthRequest): Response<AuthResponse> {
            return Response.success(AuthResponse("access", "refresh", null))
        }

        override suspend fun refresh(request: TokenRefreshRequest): Response<TokenRefreshResponse> {
            return Response.success(TokenRefreshResponse("access"))
        }
    }

    private class FakeTokenStore : TokenStorage {
        override fun getAccessToken(): String? = null
        override fun getRefreshToken(): String? = null
        override fun hasTokens(): Boolean = false
        override fun saveTokens(accessToken: String, refreshToken: String?) = Unit
        override fun clear() = Unit
    }

    private class FakeAgenticPlanGateway : AgenticPlanGateway {
        override suspend fun generatePlan(request: AgenticPlanRequest): AgenticApiResult<WellnessPlanResponse> {
            return AgenticApiResult.Error(AgenticApiErrorType.Unknown, "unused")
        }

        override suspend fun plans(): AgenticApiResult<List<WellnessPlanResponse>> {
            return AgenticApiResult.Success(emptyList())
        }

        override suspend fun plan(id: Long): AgenticApiResult<WellnessPlanResponse> {
            return AgenticApiResult.Error(AgenticApiErrorType.NotFound, "unused")
        }

        override suspend fun planStatus(id: Long): AgenticApiResult<WellnessPlanStatusResponse> {
            return AgenticApiResult.Error(AgenticApiErrorType.NotFound, "unused")
        }

        override suspend fun finalReport(id: Long): AgenticApiResult<FinalReportResponse> {
            return AgenticApiResult.Error(AgenticApiErrorType.ReportNotReady, "unused")
        }
    }

    private class FakePatientProfileGateway : PatientProfileGateway {
        override suspend fun loadProfile(): PatientProfileLoadResult {
            return PatientProfileLoadResult.Cache(
                StoredPatientProfile("", "", "", "", ""),
                "unused"
            )
        }

        override suspend fun saveProfile(profile: StoredPatientProfile): AgenticApiResult<StoredPatientProfile> {
            return AgenticApiResult.Success(profile)
        }
    }
}
