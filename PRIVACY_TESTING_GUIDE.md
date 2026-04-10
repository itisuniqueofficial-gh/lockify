# Lockify Privacy Testing Guide

## Overview
This guide provides step-by-step instructions for testing all privacy and security fixes in Lockify.

## Prerequisites
- Lockify app installed on test device
- At least 2-3 apps added to protected apps list
- Accessibility service enabled
- Test device with Android 8.0+ (recommended: test on multiple Android versions)

## Test Environment Setup

### 1. Enable Lockify Protection
1. Open Lockify app
2. Set a PIN or Pattern lock
3. Add test apps to protected list (e.g., Chrome, Gmail, Calculator)
4. Enable Accessibility Service
5. Enable "Protect Apps" toggle in settings

### 2. Configure Test Settings
- Disable "Unlock Time Duration" (set to 0) for strict testing
- Enable/disable biometric auth as needed for specific tests
- Keep logging enabled during testing for debugging

## Privacy Test Suite

### Test Category 1: Protected App Launch Privacy

#### Test 1.1: Launch from Launcher
**Objective**: Verify no content visible before authentication

**Steps**:
1. Ensure protected app is not running
2. Tap protected app icon from launcher
3. Observe transition carefully

**Expected Result**:
- Lock screen appears immediately
- No glimpse of app content
- No flicker or flash of protected app
- Smooth transition to lock screen

**Pass Criteria**: Zero visible content before lock screen

---

#### Test 1.2: Launch from App Drawer
**Objective**: Verify protection works from app drawer

**Steps**:
1. Open app drawer
2. Tap protected app
3. Observe transition

**Expected Result**:
- Lock screen appears immediately
- No content leak

**Pass Criteria**: Zero visible content before lock screen

---

### Test Category 2: Recent Apps Privacy

#### Test 2.1: Minimize Protected App
**Objective**: Verify no content visible when minimizing

**Steps**:
1. Open and unlock a protected app
2. Press Home button or use gesture to minimize
3. Observe the transition

**Expected Result**:
- Smooth transition to home screen
- No content visible during transition
- App minimizes cleanly

**Pass Criteria**: No content leak during minimize

---

#### Test 2.2: View Recent Apps
**Objective**: Verify protected app thumbnail is secure

**Steps**:
1. Open and unlock a protected app
2. Use app for a few seconds
3. Open Recent Apps (square button or swipe up gesture)
4. Look at the protected app's thumbnail

**Expected Result**:
- Protected app thumbnail shows lock screen OR blank screen
- No actual app content visible in thumbnail
- Other unprotected apps show normal thumbnails

**Pass Criteria**: No app content in recent apps thumbnail

---

#### Test 2.3: Reopen from Recent Apps
**Objective**: Verify relock when returning from recents

**Steps**:
1. Open and unlock a protected app
2. Press Home button
3. Open Recent Apps
4. Tap the protected app thumbnail

**Expected Result**:
- Lock screen appears immediately
- Must re-authenticate
- No content visible before authentication

**Pass Criteria**: Requires re-authentication, no content leak

---

### Test Category 3: App Switching Privacy

#### Test 3.1: Switch to Another App
**Objective**: Verify unlock state cleared on app switch

**Steps**:
1. Open and unlock protected app A
2. Press Home button
3. Open another app (protected or unprotected)
4. Return to protected app A

**Expected Result**:
- Lock screen appears when returning to app A
- Must re-authenticate

**Pass Criteria**: Requires re-authentication

---

#### Test 3.2: Fast App Switching
**Objective**: Verify no leaks during rapid switching

**Steps**:
1. Open and unlock protected app A
2. Quickly switch to protected app B (via recents)
3. Quickly switch back to app A
4. Repeat several times rapidly

**Expected Result**:
- Lock screen appears for each protected app
- No content glimpses during fast switching
- No crashes or lag

**Pass Criteria**: Smooth operation, no content leaks

---

#### Test 3.3: Switch Between Protected Apps
**Objective**: Verify each app requires separate authentication

**Steps**:
1. Open and unlock protected app A
2. Press Home
3. Open protected app B
4. Authenticate app B
5. Return to app A via recents

**Expected Result**:
- App B requires authentication
- App A requires re-authentication when returned to
- Each app maintains separate lock state

**Pass Criteria**: Each app requires separate auth

---

### Test Category 4: Screen Lock Privacy

#### Test 4.1: Screen Off/On
**Objective**: Verify unlock state cleared on screen off

**Steps**:
1. Open and unlock a protected app
2. Press power button to turn screen off
3. Wait 2 seconds
4. Press power button to turn screen on
5. Unlock device
6. Protected app should be visible in foreground

**Expected Result**:
- Lock screen appears immediately
- Must re-authenticate
- No content visible

**Pass Criteria**: Requires re-authentication after screen off

---

#### Test 4.2: Screen Off During Lock Screen
**Objective**: Verify lock screen state preserved

**Steps**:
1. Open protected app (lock screen appears)
2. Press power button before authenticating
3. Turn screen back on
4. Unlock device

**Expected Result**:
- Lock screen still present
- Can continue authentication
- No content leak

**Pass Criteria**: Lock screen preserved, no content visible

---

### Test Category 5: Configuration Changes

#### Test 5.1: Rotation During Lock Screen
**Objective**: Verify no content leak during rotation

**Steps**:
1. Open protected app (lock screen appears)
2. Rotate device 90 degrees
3. Observe transition

**Expected Result**:
- Lock screen rotates smoothly
- No glimpse of protected app content
- Authentication state preserved

**Pass Criteria**: No content leak during rotation

---

#### Test 5.2: Rotation After Unlock
**Objective**: Verify unlock state preserved during rotation

**Steps**:
1. Open and unlock protected app
2. Rotate device
3. Use app normally

**Expected Result**:
- App rotates normally
- Unlock state preserved
- No unexpected relock

**Pass Criteria**: Smooth rotation, state preserved

---

### Test Category 6: Biometric Authentication

#### Test 6.1: Biometric Success
**Objective**: Verify biometric auth works without content leak

**Steps**:
1. Enable biometric authentication in settings
2. Open protected app
3. Use fingerprint/face to authenticate

**Expected Result**:
- Biometric prompt appears
- On success, app unlocks smoothly
- No content visible before authentication

**Pass Criteria**: Smooth biometric auth, no content leak

---

#### Test 6.2: Biometric Cancel
**Objective**: Verify fallback to PIN works

**Steps**:
1. Open protected app
2. Cancel biometric prompt
3. Enter PIN/pattern

**Expected Result**:
- Can fallback to PIN/pattern
- No content visible
- Lock screen remains secure

**Pass Criteria**: Fallback works, no content leak

---

### Test Category 7: Screenshot & Screen Recording

#### Test 7.1: Screenshot Lock Screen
**Objective**: Verify lock screen cannot be captured

**Steps**:
1. Open protected app (lock screen appears)
2. Attempt to take screenshot (Power + Volume Down)

**Expected Result**:
- Screenshot blocked or shows black screen
- System may show "Can't take screenshot" message
- Lock screen content not captured

**Pass Criteria**: Screenshot blocked or shows blank

---

#### Test 7.2: Screen Recording
**Objective**: Verify screen recording protection

**Steps**:
1. Start screen recording
2. Open protected app
3. Stop recording
4. View recording

**Expected Result**:
- Lock screen appears as black or blank in recording
- Protected app content not visible in recording
- Recording continues but content protected

**Pass Criteria**: Protected content not in recording

---

### Test Category 8: Grace Period (if enabled)

#### Test 8.1: Quick Return Within Grace Period
**Objective**: Verify grace period works correctly

**Steps**:
1. Set unlock duration to 1 minute in settings
2. Open and unlock protected app
3. Press Home immediately
4. Reopen app within 5 seconds

**Expected Result**:
- App opens without requiring re-authentication
- Grace period working as intended

**Pass Criteria**: No re-auth within grace period

---

#### Test 8.2: Return After Grace Period
**Objective**: Verify relock after grace period expires

**Steps**:
1. Set unlock duration to 1 minute
2. Open and unlock protected app
3. Press Home
4. Wait 2 minutes
5. Reopen app

**Expected Result**:
- Lock screen appears
- Must re-authenticate
- Grace period expired correctly

**Pass Criteria**: Requires re-auth after grace period

---

### Test Category 9: Edge Cases

#### Test 9.1: Multiple Rapid Opens
**Objective**: Verify stability under stress

**Steps**:
1. Rapidly open and close protected app 10 times
2. Authenticate each time

**Expected Result**:
- No crashes
- Lock screen appears each time
- Smooth operation

**Pass Criteria**: Stable operation, no crashes

---

#### Test 9.2: Low Memory Scenario
**Objective**: Verify protection after process death

**Steps**:
1. Open and unlock protected app
2. Open many other apps to trigger low memory
3. Return to protected app

**Expected Result**:
- Lock screen appears if app was killed
- No content visible
- Protection restored after process recreation

**Pass Criteria**: Protection works after process death

---

#### Test 9.3: Notification Interaction
**Objective**: Verify notifications don't bypass lock

**Steps**:
1. Ensure protected app is locked
2. Receive notification from protected app
3. Tap notification

**Expected Result**:
- Lock screen appears
- Must authenticate before accessing app
- Notification doesn't bypass lock

**Pass Criteria**: Lock screen appears, no bypass

---

### Test Category 10: Anti-Uninstall (if enabled)

#### Test 10.1: Uninstall Attempt
**Objective**: Verify anti-uninstall protection

**Steps**:
1. Enable anti-uninstall for a protected app
2. Long-press app icon
3. Attempt to uninstall

**Expected Result**:
- Uninstall blocked or requires authentication
- No content leak during uninstall flow
- Protection maintained

**Pass Criteria**: Uninstall protected, no content leak

---

## Performance Testing

### Performance Test 1: Launch Speed
**Objective**: Verify lock screen appears quickly

**Steps**:
1. Open protected app
2. Measure time to lock screen appearance

**Expected Result**:
- Lock screen appears within 200-500ms
- No visible delay
- Smooth transition

**Pass Criteria**: Fast appearance, smooth UX

---

### Performance Test 2: Battery Impact
**Objective**: Verify minimal battery drain

**Steps**:
1. Use device normally for 1 hour with Lockify enabled
2. Check battery usage in settings

**Expected Result**:
- Lockify uses minimal battery (<2% per hour)
- No excessive background activity
- Efficient operation

**Pass Criteria**: Low battery usage

---

## Regression Testing

After any code changes, run this quick regression suite:

1. ✅ Launch protected app from launcher - no content leak
2. ✅ View recent apps - no content in thumbnail
3. ✅ Reopen from recents - requires re-auth
4. ✅ Screen off/on - requires re-auth
5. ✅ Rotate device - no content leak
6. ✅ Fast app switching - no content leak
7. ✅ Screenshot attempt - blocked
8. ✅ Multiple protected apps - each requires auth

## Test Result Template

```
Test Date: [DATE]
Device: [DEVICE MODEL]
Android Version: [VERSION]
Lockify Version: [VERSION]

Test Results:
- Protected App Launch: PASS/FAIL
- Recent Apps Privacy: PASS/FAIL
- App Switching: PASS/FAIL
- Screen Lock: PASS/FAIL
- Configuration Changes: PASS/FAIL
- Biometric Auth: PASS/FAIL
- Screenshot Protection: PASS/FAIL
- Grace Period: PASS/FAIL
- Edge Cases: PASS/FAIL
- Performance: PASS/FAIL

Issues Found:
[List any issues]

Notes:
[Additional observations]
```

## Automated Testing Recommendations

For CI/CD integration, consider:
1. UI Automator tests for launch scenarios
2. Espresso tests for lock screen UI
3. Screenshot comparison tests
4. Performance benchmarks
5. Memory leak detection

## Conclusion

This comprehensive test suite ensures Lockify provides:
- Zero content leakage
- Zero preview leaks
- Zero state leaks
- Production-ready privacy
- Smooth performance

Run all tests before each release to maintain privacy and security standards.
