use sgdb;
DELIMITER $$

CREATE TRIGGER update_room_occupancy
AFTER INSERT ON student_info
FOR EACH ROW
BEGIN
    UPDATE room
    SET current_occupancy = current_occupancy + 1
    WHERE room_id = NEW.room_id
    AND current_occupancy < capacity;
END$$

DELIMITER ;