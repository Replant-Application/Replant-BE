import sys
import subprocess

# Install pymysql if not present
try:
    import pymysql
except ImportError:
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'pymysql'])
    import pymysql

def check_and_add_columns():
    print("Connecting to database...")
    try:
        conn = pymysql.connect(
            host='113.198.66.75',
            port=13150,
            user='replant_admin',
            password='replant2025',
            database='replant',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
    except Exception as e:
        print(f"Connection failed: {e}")
        return

    try:
        with conn.cursor() as cursor:
            # Check todolist table columns
            print("Checking columns in todolist table...")
            cursor.execute("DESCRIBE todolist")
            existing_cols = [row['Field'] for row in cursor.fetchall()]
            
            # Map of column_name -> definition
            needed_columns = {
                'deleted_at': 'DATETIME DEFAULT NULL',
                'is_active': 'BIT(1) DEFAULT 1' # Check if is_active is missing too
                # Add others if needed from error log
                # The error log listed: added_count, average_rating, completed_count, created_at, creator_id, deleted_at, description, is_active, is_public, review_count, set_type, title, todolist_status, total_count, updated_at
            }
            
            for col_name, definition in needed_columns.items():
                if col_name not in existing_cols:
                    print(f"Adding missing column: {col_name}...")
                    try:
                        sql = f"ALTER TABLE todolist ADD COLUMN {col_name} {definition}"
                        print(f"Executing: {sql}")
                        cursor.execute(sql)
                        print("  -> Done.")
                    except Exception as e:
                        print(f"  -> Failed: {e}")
                else:
                    print(f"Column '{col_name}' already exists.")
            
        conn.commit()
        print("Schema update check completed.")

    except Exception as e:
        print(f"Schema check error: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    check_and_add_columns()
