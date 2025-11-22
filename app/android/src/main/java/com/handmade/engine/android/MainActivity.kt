package com.handmade.engine.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.handmade.engine.decision.DecisionEngineImpl
import com.handmade.engine.decision.EngineInput
import com.handmade.engine.domain.AnalyzeResult
import com.handmade.engine.domain.CraftType
import com.handmade.engine.domain.MaskSet

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val decisionEngine = DecisionEngineImpl(gates = emptyList(), mlAdvisor = null)
        val input = EngineInput(
            analysis = AnalyzeResult(),
            masks = MaskSet(),
            craftType = CraftType.CROSS_STITCH,
        )

        val output = decisionEngine.run(input)
        output.candidates.firstOrNull()?.let { plan ->
            Log.d("HandmadeEngine", "Selected plan: ${'$'}plan")
        }
    }
}
