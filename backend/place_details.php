<?php
require_once 'config.php';
header('Content-Type: application/json');
$place = $_GET['place'] ?? '';

if (empty($place)) {
    die(json_encode(["status" => "error", "message" => "No place specified"]));
}

$cache_file = "cache/" . md5(strtolower($place)) . ".json";
$cache_time = 7 * 24 * 60 * 60; // 7 days

if (file_exists($cache_file) && (time() - filemtime($cache_file) < $cache_time)) {
    $cached_data = json_decode(file_get_contents($cache_file), true);
    if ($cached_data) {
        echo json_encode([
            "status" => "success",
            "data" => $cached_data,
            "cached" => true
        ]);
        exit;
    }
}

$prompt = "Provide travel information for " . $place . " in strict JSON format. 
Fields required:
- description (detailed)
- hotels (list of top 3 nearby hotels)
- travel_methods (best way to travel there)
- budget_min (minimum daily budget in INR)
- budget_max (maximum daily budget in INR)
- latitude (approximate)
- longitude (approximate)
- best_time (best months to visit)
- rating (out of 5.0)
- recommended_places (list of top 4-5 recommended attractions or places to visit)

Return ONLY JSON.";

$ai_response = callGemini($prompt, true);
$ai_data = null;

if (isset($ai_response['success'])) {
    $ai_data = json_decode(trim($ai_response['text']), true);
}

if ($ai_data) {
    $final_data = [
        "title" => $place,
        "description" => $ai_data['description'] ?? "",
        "hotels" => $ai_data['hotels'] ?? [],
        "travel_methods" => $ai_data['travel_methods'] ?? "",
        "budget_min" => (string)($ai_data['budget_min'] ?? "0"),
        "budget_max" => (string)($ai_data['budget_max'] ?? "0"),
        "latitude" => (float)($ai_data['latitude'] ?? 0),
        "longitude" => (float)($ai_data['longitude'] ?? 0),
        "best_time" => is_array($ai_data['best_time']) ? implode(", ", $ai_data['best_time']) : ($ai_data['best_time'] ?? "All year"),
        "rating" => (string)($ai_data['rating'] ?? "4.5"),
        "recommended_places" => $ai_data['recommended_places'] ?? []
    ];
    
    // Save to cache
    if (!is_dir('cache')) {
        mkdir('cache', 0777, true);
    }
    file_put_contents($cache_file, json_encode($final_data));
    
    echo json_encode([
        "status" => "success",
        "data" => $final_data
    ]);
    exit;
}

// Fallback to Wikipedia + Static data if API limit hit or failed
$wiki_url = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=" . urlencode($place) . "&format=json&redirects=1";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $wiki_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_USERAGENT, 'TravelBuddyApp/1.0');
curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Don't let Wikipedia call hang indefinitely
$response = curl_exec($ch);
curl_close($ch);

$description = "Explore the beautiful wonders of " . $place . ". This destination offers a unique blend of culture and history.";
if ($response) {
    $data = json_decode($response, true);
    $pages = $data['query']['pages'] ?? [];
    $page = @reset($pages);
    if (!empty($page['extract'])) {
        $description = $page['extract'];
    }
}

$fallback_data = [
    "title" => $place,
    "description" => $description,
    "hotels" => ["Grand Palace Hotel", "Ocean View Resort", "City Heritage Inn"],
    "travel_methods" => "Flights and local trains are recommended.",
    "budget_min" => "4000",
    "budget_max" => "20000",
    "latitude" => 13.0827,
    "longitude" => 80.2707,
    "best_time" => "September to March",
    "rating" => "4.8/5",
    "recommended_places" => ["Local Market", "Historic Temple", "Scenic Beach", "City Park"]
];

// Save the fallback data to the cache so subsequent requests are instantaneous!
if (!is_dir('cache')) {
    mkdir('cache', 0777, true);
}
file_put_contents($cache_file, json_encode($fallback_data));

echo json_encode([
    "status" => "success",
    "data" => $fallback_data
]);
exit;
?>
