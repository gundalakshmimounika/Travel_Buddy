<?php
include 'db_config.php';

$email = $_POST['email'] ?? '';
$destination = $_POST['destination'] ?? '';
$image_res = $_POST['image_res'] ?? 0;

if (!empty($email) && !empty($destination)) {
    $stmt = $conn->prepare("INSERT IGNORE INTO planned_trips (user_email, destination, image_res) VALUES (?, ?, ?)");
    $stmt->bind_param("ssi", $email, $destination, $image_res);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Trip added"]);
    } else {
        echo json_encode(["status" => "error", "message" => $stmt->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Missing data"]);
}
$conn->close();
?>
