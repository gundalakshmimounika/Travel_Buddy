<?php
include 'db_config.php';

$email = $_POST['email'] ?? '';
$destination = $_POST['destination'] ?? '';

if (!empty($email) && !empty($destination)) {
    $stmt = $conn->prepare("DELETE FROM planned_trips WHERE user_email = ? AND destination = ?");
    $stmt->bind_param("ss", $email, $destination);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Trip deleted"]);
    } else {
        echo json_encode(["status" => "error", "message" => $stmt->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Missing data"]);
}
$conn->close();
?>
