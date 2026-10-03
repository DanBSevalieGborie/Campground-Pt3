# Android Unit 6 Lab - Campground Explorer Pt. 3: Fragments and Navigation

Submitted by: **Dan Sevalie-Gborie**

**Parks and Campgrounds** combines the National Parks app (Unit 3) and the Campground Explorer app (Units 4–5) into one app. A bottom navigation bar switches between a **Parks** tab and a **Campgrounds** tab. Each tab is a fragment that loads live data from the National Park Service API, and tapping a card opens a detail screen with a photo gallery and everything the API knows about that place.

The Campgrounds side was rebuilt to match the modern look of the National Parks app: the same Material 3 photo cards, forest-green theme, search, and a full detail screen.

Time spent: **X** hours spent in total

## Required Features

The following **required** functionality is completed:

- [x] Add and implement Bottom Navigation to your application
  - `BottomNavigationView` in `activity_main.xml`, with its two items in `menu/bottom_navigation_menu.xml`
- [x] Dynamically use fragments within Activities
  - `MainActivity` swaps `ParksFragment` and `CampgroundFragment` into the `main_frame_layout` FrameLayout with a `replaceFragment()` helper when a tab is selected
- [x] Customize the toolbar and icons
  - A forest-green `MaterialToolbar` whose title follows the selected tab ("National Parks" / "Campgrounds") and holds each tab's search box
  - Vector tab icons from Material Symbols (`park` and `camping`, fill `#222`): filled when the tab is selected, outlined when it isn't, with a green pill behind the selected tab
  - A new launcher icon: a pine and a tent

## Stretch Features

The following **stretch** features are implemented:

- [ ] Add and implement options in the Settings screen
- [ ] Customize a Home screen using the Most Popular NPS Campgrounds API
- [x] Respond to device orientation changes without resetting the application
  - Each tab's data lives in a ViewModel scoped to `MainActivity`, so rotating doesn't refetch anything
  - The selected tab, toolbar title, search text and scroll position all survive a rotation

## Additional Features

- [x] **Modern campground cards:** big photo with the nightly price on it ("$8 / night"), campground type and number of sites, name, description, and the town it's in — the same card style as the parks
- [x] **Campground detail screen** (it used to be a plain image + text): swipeable photo gallery with captions and credits, **Directions** (Google Maps) and **Reserve** (recreation.gov) buttons, cards for the camping fee and number of campsites, amenity chips (drinking water, showers, RV length...), reservations, hours, weather and directions
- [x] **Park detail screen** carried over from the National Parks app: gallery, entrance fee, things to do, weather, directions and website
- [x] **Search on both tabs:** parks by name, designation or state; campgrounds by name, description, town or state (every word must match, like Lab 5)
- [x] Switching tabs keeps each tab's data, search and scroll position, so nothing reloads
- [x] Tapping the tab you're already on scrolls its list back to the top
- [x] **Pull to refresh** on both lists
- [x] Error messages with a **Retry** button instead of a blank screen (offline, bad API key and rate limit each get their own message); a failed refresh keeps the old list and shows a Snackbar
- [x] Light and dark mode
- [x] 21 unit tests for the JSON parsing, fee choice, amenities and search rules (`./gradlew test`)

## Video Walkthrough

Here's a walkthrough of implemented user stories:

<!-- Replace with your GIF or video link -->
<img src='walkthrough.gif' title='Video Walkthrough' width='' alt='Video Walkthrough' />

GIF created with ...

## How it's built

| File | What it does |
| --- | --- |
| `MainActivity.kt` | Toolbar + bottom navigation; swaps the fragments with `replaceFragment()` |
| `ParksFragment.kt` / `CampgroundFragment.kt` | Each tab: list, search box, pull to refresh, loading/error states |
| `NpsListViewModel.kt` | Fetches an NPS endpoint with AsyncHttpClient and holds the results (`ParksViewModel`, `CampgroundsViewModel`) |
| `NationalPark.kt` / `Campground.kt` | The models, parsed from the API's JSON |
| `ParksAdapter.kt` / `CampgroundAdapter.kt` | `ListAdapter`s for the cards (DiffUtil animates search changes) |
| `ParkDetailActivity.kt` / `CampgroundDetailActivity.kt` | The detail screens |
| `PhotoGallery.kt` | The swipeable photo gallery both detail screens share |
| `ParkSearch.kt` | The search rules for both tabs |
| `SearchMenu.kt` | Adds a tab's search box to the toolbar while that tab is showing |

## Notes

The lab's sample code sets `bottomNavigationView.selectedItemId = R.id.nav_parks` every time `onCreate` runs. That resets the app to the Parks tab on every rotation, so here the starting tab is only loaded on a fresh start (`savedInstanceState == null`). After a rotation the FragmentManager and the bottom bar restore the tab the user was on.

Setting `selectedItemId` also stopped working once tapping the current tab scrolled to the top: Parks is already selected when the app opens, so selecting it again counts as a *reselect*, which never loads the fragment — the app opened to a blank screen. `MainActivity` now loads the starting tab's fragment directly.

`replace()` destroys the fragment you leave, so keeping the data in the fragment would mean a new network request on every tab switch. Moving it into ViewModels scoped to the activity fixed that, and it also handles rotation.

Each tab adds its own search box to the toolbar. When you switch tabs, the toolbar collapses the old search box, and collapsing a `SearchView` clears its text — which wiped the search you'd typed. The fix was to ignore text changes while the fragment isn't resumed.

The campground API has a lot more than the old app used. The trickiest part was the fee: many campgrounds list a discounted Senior/Access Pass rate or a group site first, so the app skips those and shows the standard site fee. A few links also come back without `https://`, which Android can't open, so those get it added.

## Setup

Create `apikey.properties` in the project's root folder containing:

```
API_KEY=<YOUR_NPS_API_KEY>
```

`apikey.properties` is git-ignored, so the key isn't committed. Without it the app falls back to NPS's shared `DEMO_KEY`, which works but is rate-limited.

## License

    Copyright [2026] [Dan Sevalie-Gborie]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
