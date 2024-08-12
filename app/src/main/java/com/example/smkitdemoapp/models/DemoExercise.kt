package com.example.smkitdemoapp.models

enum class DemoExercise(var count: Int = 0) {
    PushupRegular,
    SquatJump,
    SquatRegular,
    SquatAndRotationJab,
    SquatSide,
    SideStepJacks,
    Lunge,
    PlankHighStatic,
    PlankLowStatic,
    SquatRegularOverheadStatic,
    SquatRegularStatic,
    StandingKneeRaiseRight,
    StandingKneeRaiseLeft,
    StandingSideBendRight,
    StandingSideBendLeft,
    InnerThighMobility,
    HipExternalRotationGlutesMobility,
    HipInternalRotationMobility,
    HipFlexionMobility,
    OverheadMobility,
    PecsMobility;

    companion object {
        val allValues: List<DemoExercise> = values().toList()
    }
}
