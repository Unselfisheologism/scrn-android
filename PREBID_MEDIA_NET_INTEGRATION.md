# Media.net Ads Integration with Prebid Mobile SDK

This document describes the integration of Media.net ads using the Prebid Mobile SDK in the MNML Screen Recorder app.

## Overview

The integration implements:
- **Banner ads** at the bottom of the main activity layout
- **Interstitial ads** that appear after recording stops (non-intrusive)
- **Premium user check** to optionally hide ads for premium users

## Dependencies Added

### Gradle Dependencies
Added to `/home/engine/project/home/engine/project/build.gradle`:
```gradle
// Prebid Mobile SDK for Media.net ads
implementation 'org.prebid:prebid-mobile-sdk:2.1.8'
```

## Configuration

### String Resources
Added to `/home/engine/project/app/src/main/res/values/strings.xml`:
```xml
<!-- Prebid Mobile SDK configuration for Media.net ads -->
<string name="prebid_config_id">YOUR_PREBID_CONFIG_ID</string>
<string name="banner_ad_unit_id">YOUR_BANNER_AD_UNIT_ID</string>
<string name="banner_config_id">YOUR_BANNER_CONFIG_ID</string>
<string name="interstitial_ad_unit_id">YOUR_INTERSTITIAL_AD_UNIT_ID</string>
<string name="interstitial_config_id">YOUR_INTERSTITIAL_CONFIG_ID</string>
```

**Important**: Replace the placeholder values with your actual Media.net/Prebid configuration IDs:
- `prebid_config_id`: Your Prebid Server configuration ID
- `banner_ad_unit_id`: Your banner ad unit ID
- `banner_config_id`: Your banner configuration ID  
- `interstitial_ad_unit_id`: Your interstitial ad unit ID
- `interstitial_config_id`: Your interstitial configuration ID

## Layout Changes

### Banner Ad Container
Added to `/home/engine/project/app/src/main/res/layout/activity_main.xml`:
```xml
<!-- Banner ad container -->
<FrameLayout
    android:id="@+id/ad_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    android:layout_marginBottom="72dp"
    android:minHeight="50dp"
    android:background="#F5F5F5"
    android:visibility="gone"
    />
```

## Implementation Details

### MainActivity.kt Changes

#### Imports Added
```kotlin
// Prebid Mobile SDK for Media.net ads
import org.prebid.mobile.AdUnit
import org.prebid.mobile.AdSize
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.addendum.AdViewUtils
import org.prebid.mobile.Result
import kotlinx.android.synthetic.main.activity_main.ad_container
```

#### Properties Added
```kotlin
// Prebid Mobile SDK ad units for Media.net integration
private var bannerAdUnit: AdUnit? = null
private var interstitialAdUnit: AdUnit? = null
```

#### Initialization
The ads are initialized in `initializeAds()` method called from `onCreate()`:
- Prebid Mobile SDK initialization
- Media.net bidder configuration
- Banner and interstitial ad setup

#### Banner Ad Integration
- Creates AdUnit for banner ads (320x50 size)
- Fetches demand from Media.net via Prebid
- Shows ad container when ad is ready

#### Interstitial Ad Integration
- Creates AdUnit for interstitial ads
- Fetches demand from Media.net via Prebid
- Shows interstitial after recording stops (non-intrusive timing)

#### Recording State Tracking
The app tracks when recording stops by monitoring the FAB icon changes:
- When FAB changes from stop icon to record icon, recording has stopped
- At this point, interstitial ad is triggered (if not premium user)

## Usage

### Banner Ads
Banner ads are loaded automatically when the app starts and displayed at the bottom of the main screen.

### Interstitial Ads
Interstitial ads are shown after a recording session completes, providing a non-intrusive user experience.

### Premium User Check
The `isPremiumUser()` method should be implemented to check if the current user has premium status. Currently returns `false` as a placeholder.

## Integration Steps for Production

1. **Configure Prebid Server**: Set up your Prebid Server with Media.net as a bidder
2. **Get Configuration IDs**: Obtain the actual configuration IDs from your Prebid setup
3. **Update String Resources**: Replace all placeholder IDs in `strings.xml`
4. **Implement Premium Check**: Update `isPremiumUser()` method with your app's premium logic
5. **Test Integration**: Test both banner and interstitial ads in staging environment
6. **Monitor Performance**: Use Prebid analytics to monitor ad performance

## Key Features

- **Non-intrusive timing**: Interstitial only shows after user-initiated recording stops
- **Premium user support**: Ads can be hidden for premium users
- **Proper lifecycle management**: Ads are cleaned up when activity is destroyed
- **Efficient demand fetching**: Uses Prebid's demand fetching for optimal monetization

## Permissions

The `INTERNET` permission was already present in `AndroidManifest.xml`, which is required for ad serving.

## Next Steps

To complete the integration:
1. Replace placeholder configuration IDs with real ones
2. Implement proper premium user checking
3. Add error handling for ad loading failures
4. Consider adding ad loading states and retry logic
5. Test with real Media.net demand partners