<?php
require_once 'config.php';

header('Content-Type: application/json');

$place = $_GET['place'] ?? '';

if (empty($place)) {
    die(json_encode(["status" => "error", "message" => "No place specified"]));
}

$cache_file = "cache/hotels_" . md5(strtolower($place)) . ".json";
$cache_time = 7 * 24 * 60 * 60; // 7 days

if (file_exists($cache_file) && (time() - filemtime($cache_file) < $cache_time)) {
    $cached_data = file_get_contents($cache_file);
    if (!empty($cached_data)) {
        echo trim($cached_data);
        exit;
    }
}

$prompt = "List 5 top-rated real hotels in $place. 
For each hotel, provide:
1. Hotel Name
2. Rating (out of 5)
3. Starting Price per night (in INR)
4. A short professional description
5. Contact Number (mock if unknown)

Return ONLY a valid JSON array of objects with these keys: 
name, rating, price, description, contact.
Example: [{\"name\": \"Hotel Bali\", \"rating\": \"4.5\", \"price\": \"3500\", \"description\": \"Beachfront paradise...\", \"contact\": \"+91 9876543210\"}]";

$response = callGemini($prompt, true);
$hotels_data = null;

if ($response['success']) {
    $clean_json = trim($response['text']);
    $hotels_data = json_decode($clean_json, true);
}

if ($hotels_data !== null && is_array($hotels_data)) {
    if (!is_dir('cache')) {
        mkdir('cache', 0777, true);
    }
    $final_output = json_encode($hotels_data);
    file_put_contents($cache_file, $final_output);
    echo $final_output;
    exit;
}

// Fallback if API fails or returns invalid response
$fallback_hotels = [
    [
        "name" => "Grand Palace Hotel",
        "rating" => "4.6",
        "price" => "4500",
        "description" => "Experience luxury in the heart of $place with premium amenities and dining.",
        "contact" => "+91 9876543210"
    ],
    [
        "name" => "Ocean View Resort",
        "rating" => "4.5",
        "price" => "6000",
        "description" => "Stunning oceanfront rooms with direct beach access and private pools.",
        "contact" => "+91 8765432109"
    ],
    [
        "name" => "City Heritage Inn",
        "rating" => "4.2",
        "price" => "3000",
        "description" => "Cozy, traditional accommodations close to historic sites and shopping.",
        "contact" => "+91 7654321098"
    ],
    [
        "name" => "Green Meadows Stay",
        "rating" => "4.3",
        "price" => "2500",
        "description" => "A peaceful garden escape perfect for nature lovers and families.",
        "contact" => "+91 6543210987"
    ],
    [
        "name" => "Boutique Comfort Inn",
        "rating" => "4.4",
        "price" => "3800",
        "description" => "Modern minimalist lodging offering exceptional service and free breakfast.",
        "contact" => "+91 5432109876"
    ]
];

if (!is_dir('cache')) {
    mkdir('cache', 0777, true);
}
$final_output = json_encode($fallback_hotels);
file_put_contents($cache_file, $final_output);
echo $final_output;
exit;
?>
