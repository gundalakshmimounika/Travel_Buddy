<?php
include 'db_config.php';

$email = $_GET['email'] ?? '';
$trips = [];

if (!empty($email)) {
    $stmt = $conn->prepare("SELECT * FROM planned_trips WHERE user_email = ? ORDER BY created_at DESC");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();
    
    while($row = $result->fetch_assoc()) {
        $trips[] = $row;
    }
    $stmt->close();
}

echo json_encode(["status" => "success", "data" => $trips]);
$conn->close();
?>
