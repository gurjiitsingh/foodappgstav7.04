package com.it10x.foodappgstav7_04.ui.settings

import com.it10x.foodappgstav7_04.data.PrinterConfig
import com.it10x.foodappgstav7_04.data.PrinterRole

data class PrinterSettingsState(
    val printers: Map<PrinterRole, PrinterConfig> = emptyMap()
)
