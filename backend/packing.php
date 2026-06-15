<?php
require_once 'db_config.php';

// Ensure packing_list table exists
$table_sql = "CREATE TABLE IF NOT EXISTS packing_list (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trip_title VARCHAR(255) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_packed TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)";
$conn->query($table_sql);

$action = $_GET['action'] ?? '';
$trip = $_GET['trip'] ?? '';

if (empty($trip)) {
    die(json_encode(["status" => "error", "message" => "No trip specified"]));
}

if ($action == 'get') {
    $stmt = $conn->prepare("SELECT * FROM packing_list WHERE trip_title = ?");
    $stmt->bind_param("s", $trip);
    $stmt->execute();
    $result = $stmt->get_result();
    $items = [];
    while ($row = $result->fetch_assoc()) {
        // Convert is_packed to boolean for easier handling in Android
        $row['is_packed'] = (bool)$row['is_packed'];
        $items[] = $row;
    }

    // If empty, seed with default items for this specific trip
    if (empty($items)) {
        $defaults = [
            ['Clothes', 'T-shirts (5)'], ['Clothes', 'Jeans (2)'], ['Clothes', 'Sneakers'],
            ['Documents', 'Passport'], ['Documents', 'Flight Tickets'], ['Documents', 'Hotel Booking'],
            ['Gadgets', 'Phone Charger'], ['Gadgets', 'Power Bank']
        ];
        foreach ($defaults as $d) {
            $istmt = $conn->prepare("INSERT INTO packing_list (trip_title, category, item_name) VALUES (?, ?, ?)");
            $istmt->bind_param("sss", $trip, $d[0], $d[1]);
            $istmt->execute();
        }
        // Fetch again after seeding
        $stmt->execute();
        $result = $stmt->get_result();
        while ($row = $result->fetch_assoc()) {
            $row['is_packed'] = (bool)$row['is_packed'];
            $items[] = $row;
        }
    }

    echo json_encode(["status" => "success", "data" => $items]);
} 
elseif ($action == 'add') {
    $item = $_GET['item'] ?? '';
    $category = $_GET['category'] ?? 'General';
    if (!empty($item)) {
        $stmt = $conn->prepare("INSERT INTO packing_list (trip_title, category, item_name) VALUES (?, ?, ?)");
        $stmt->bind_param("sss", $trip, $category, $item);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "id" => $conn->insert_id]);
        }
    }
}
elseif ($action == 'toggle') {
    $id = $_GET['id'] ?? 0;
    $status = $_GET['status'] ?? 0; // 1 for packed, 0 for unpacked
    $stmt = $conn->prepare("UPDATE packing_list SET is_packed = ? WHERE id = ?");
    $stmt->bind_param("ii", $status, $id);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    }
}
elseif ($action == 'delete') {
    $id = $_GET['id'] ?? 0;
    $stmt = $conn->prepare("DELETE FROM packing_list WHERE id = ?");
    $stmt->bind_param("i", $id);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    }
}
?>
