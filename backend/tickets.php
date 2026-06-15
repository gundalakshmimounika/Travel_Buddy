<?php
require_once 'config.php';

header('Content-Type: application/json');

$from = $_GET['from'] ?? '';
$to = $_GET['to'] ?? '';

if (empty($from) || empty($to)) {
    die(json_encode(["status" => "error", "message" => "From and To locations are required"]));
}

$cache_file = "cache/tickets_" . md5(strtolower($from . "_" . $to)) . ".json";
$cache_time = 7 * 24 * 60 * 60; // 7 days

if (file_exists($cache_file) && (time() - filemtime($cache_file) < $cache_time)) {
    $cached_data = file_get_contents($cache_file);
    if (!empty($cached_data)) {
        echo trim($cached_data);
        exit;
    }
}

$prompt = "Provide realistic travel ticket options from '$from' to '$to'.
Return ONLY a valid JSON array of objects with the following exact keys:
- type (String): Mode of transport (must be one of 'Airways', 'Train', 'Bus', 'Cab')
- price (String): Estimated realistic price formatted in INR without spaces (e.g., '₹4500')
- duration (String): Estimated realistic travel duration (e.g., '1h 20m')

Ensure exactly 3 or 4 reasonable options are provided.
Example: [{\"type\": \"Airways\", \"price\": \"₹4500\", \"duration\": \"1h 20m\"}, {\"type\": \"Train\", \"price\": \"₹850\", \"duration\": \"6h 45m\"}]";

$response = callGemini($prompt, true);
$tickets_data = null;

if ($response['success']) {
    $clean_json = trim($response['text']);
    $tickets_data = json_decode($clean_json, true);
}

if ($tickets_data !== null && is_array($tickets_data)) {
    if (!is_dir('cache')) {
        mkdir('cache', 0777, true);
    }
    $final_output = json_encode(["status" => "success", "data" => $tickets_data]);
    file_put_contents($cache_file, $final_output);
    echo $final_output;
    exit;
}

// Fallback if API fails or returns invalid response
$fallback_tickets = [
    [
        "type" => "Airways",
        "price" => "₹5500",
        "duration" => "1h 45m"
    ],
    [
        "type" => "Train",
        "price" => "₹1200",
        "duration" => "8h 30m"
    ],
    [
        "type" => "Bus",
        "price" => "₹750",
        "duration" => "10h 15m"
    ],
    [
        "type" => "Cab",
        "price" => "₹6000",
        "duration" => "7h 45m"
    ]
];

if (!is_dir('cache')) {
    mkdir('cache', 0777, true);
}
$final_output = json_encode(["status" => "success", "data" => $fallback_tickets]);
file_put_contents($cache_file, $final_output);
echo $final_output;
exit;
?>
