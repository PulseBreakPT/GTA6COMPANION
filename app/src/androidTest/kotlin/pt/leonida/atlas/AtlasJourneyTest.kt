package pt.leonida.atlas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) class AtlasJourneyTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun searchFavoriteSurvivesRecreationAndManualObjectiveWorks() {
  ui.waitUntil(20000) {ui.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty() || ui.onAllNodesWithText("Discover").fetchSemanticsNodes().isNotEmpty()}
  if(ui.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithText("Skip").performClick()
  ui.onNodeWithText("Discover",useUnmergedTree=true).performClick()
  ui.onNodeWithText("Search the atlas").performTextInput("Vice City")
  ui.onNodeWithTag("entry-vice-city").performScrollTo().performClick()
  ui.onNodeWithText("Save offline").performScrollTo().performClick()
  ui.waitUntil(10000) {ui.onAllNodesWithText("Remove from saved").fetchSemanticsNodes().isNotEmpty()}
  ui.activityRule.scenario.recreate()
  ui.waitUntil(20000) {ui.onAllNodesWithText("My vault").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("My vault").performClick()
  ui.onNodeWithText("Vice City").assertExists()
  ui.onNodeWithText("Objectives").performClick()
  ui.onNodeWithText("Create objective").performScrollTo().performClick()
  ui.onNodeWithText("Title",substring=false).performTextInput("Explore my own route")
  ui.onNodeWithText("Save",substring=false).performClick()
  ui.waitUntil(10000) {ui.onAllNodesWithText("Explore my own route").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("Explore my own route").assertExists()
 }
}
