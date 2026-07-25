package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.RatingReviewComponent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.BrandBlueDark
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.NeutralGray
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun rating_review_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          RatingReviewComponent(
            driverName = "Alieu Ceesay",
            vehicleType = "CAR",
            vehiclePlate = "BJL 4821 C",
            driverRating = 4.8f,
            tripId = "trip_dummy_123",
            onSubmitRating = { _, _, _, _ -> },
            onDismiss = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/rating_review_screenshot.png")
  }

  @Test
  fun sos_dialog_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          com.example.ui.SosEmergencyDialog(
            pLat = 13.4471,
            pLng = -16.6791,
            activeTripId = "trip_active_123",
            onDismiss = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/sos_dialog_screenshot.png")
  }

  @Test
  fun saved_places_row_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Column {
              Text(
                text = "Saved Places Preview",
                style = MaterialTheme.typography.titleMedium,
                color = BrandBlueDark
              )
              Spacer(modifier = Modifier.height(8.dp))
              
              val mockPlaces = listOf(
                com.example.data.SavedPlaceEntity(
                  id = 1L,
                  name = "Kairaba Avenue, Serrekunda",
                  label = "Home",
                  lat = 13.4471,
                  lng = -16.6791,
                  iconType = "HOME"
                ),
                com.example.data.SavedPlaceEntity(
                  id = 2L,
                  name = "Albert Market, Banjul",
                  label = "Work",
                  lat = 13.4533,
                  lng = -16.5746,
                  iconType = "WORK"
                ),
                com.example.data.SavedPlaceEntity(
                  id = 3L,
                  name = "Senegambia Strip, Kololi",
                  label = "Beach",
                  lat = 13.4380,
                  lng = -16.7120,
                  iconType = "STAR"
                )
              )
              
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(mockPlaces) { place ->
                  val icon = when (place.iconType) {
                    "HOME" -> Icons.Default.Home
                    "WORK" -> Icons.Default.Work
                    "STAR" -> Icons.Default.Star
                    else -> Icons.Default.Place
                  }
                  
                  Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = BrandBluePrimary.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(
                      1.dp,
                      BrandBluePrimary.copy(alpha = 0.15f)
                    )
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = icon,
                        contentDescription = place.label,
                        tint = BrandBluePrimary,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Column {
                        Text(
                          text = place.label,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = BrandBlueDark
                        )
                        Text(
                          text = place.name.split(",")[0],
                          fontSize = 10.sp,
                          color = NeutralGray,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/saved_places_row_screenshot.png")
  }

  @Test
  fun map_realtime_progress_hud_screenshot() {
    val dummyTrip = com.example.data.TripEntity(
      id = "trip_progress_test_123",
      passengerName = "David Otu",
      pickupName = "Albert Market, Banjul",
      dropoffName = "Kairaba Business Hub, Serrekunda",
      pickupLat = 13.4533,
      pickupLng = -16.5746,
      dropoffLat = 13.4471,
      dropoffLng = -16.6791,
      fareGmd = 350,
      paymentMethod = "FLUTTERWAVE",
      status = "ACCEPTED", // Navigating to pickup
      vehicleType = "CAR",
      driverId = "drv_test_123",
      driverName = "Modou Barrow",
      vehiclePlate = "BJL 2841 A"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            com.example.ui.WayGoMapView(
              modifier = Modifier.fillMaxWidth(),
              drivers = emptyList(),
              activeTrip = dummyTrip,
              simulatedDriverLat = 13.4500,
              simulatedDriverLng = -16.6000,
              progress = 0.18f
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/map_realtime_progress_hud_screenshot.png")
  }
}
