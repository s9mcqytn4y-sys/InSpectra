package com.primaraya.inspectra.fitur.checksheet.data

import com.primaraya.inspectra.core.network.NetworkResult
import com.primaraya.inspectra.fitur.checksheet.domain.PayloadChecksheet

interface ChecksheetRepository {
    suspend fun submitChecksheet(payload: PayloadChecksheet): NetworkResult<String>
}
