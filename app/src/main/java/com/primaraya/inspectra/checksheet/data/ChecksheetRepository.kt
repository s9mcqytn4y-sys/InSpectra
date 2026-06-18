package com.primaraya.inspectra.checksheet.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.domain.model.PayloadChecksheet

interface ChecksheetRepository {
    suspend fun submitChecksheet(payload: PayloadChecksheet): NetworkResult<String>
}
