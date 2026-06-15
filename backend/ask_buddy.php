<?php
require_once 'config.php';
header('Content-Type: application/json');

$json = file_get_contents('php://input');
$data = json_decode($json, true);
$question = $data['question'] ?? '';

if (empty($question)) {
    die(json_encode(["status" => "error", "message" => "No question specified"]));
}

$prompt = "You are 'Travel Buddy', a helpful and expert travel assistant. Answer this travel question: " . $question . ". Keep your answer concise, helpful, and adventurous. If it's not related to travel, politely redirect them back to travel topics.";

$ai_response = callGemini($prompt);

if (isset($ai_response['success'])) {
    echo json_encode([
        "status" => "success",
        "answer" => $ai_response['text']
    ]);
} else if ($ai_response['error'] === 'rate_limit') {
    echo json_encode([
        "status" => "success",
        "answer" => "Whew! I've been giving so much travel advice lately that I need a quick breather. Please ask me again in a minute!"
    ]);
} else {
    $errCode = $ai_response['code'] ?? 'Unknown';
    echo json_encode([
        "status" => "success",
        "answer" => "I'm having a little trouble connecting to my travel brain (Error: $errCode). Let's try again in a bit!"
    ]);
}
?>
