import sys
import subprocess

# Install pymysql if not present
try:
    import pymysql
except ImportError:
    print("Installing pymysql...")
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'pymysql'])
    import pymysql

def migrate():
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
            print("Disabling FK checks...")
            cursor.execute("SET FOREIGN_KEY_CHECKS=0;")
            
            # 1. Rename Tables
            tables_to_rename = [
                ("mission_set", "todolist"),
                ("mission_set_mission", "todolist_mission"),
                ("mission_set_review", "todolist_review")
            ]
            
            for old_t, new_t in tables_to_rename:
                try:
                    print(f"Renaming {old_t} to {new_t}...")
                    cursor.execute(f"RENAME TABLE {old_t} TO {new_t}")
                    print("  -> Done.")
                except Exception as e:
                    print(f"  -> Failed (maybe already renamed?): {e}")

            # 2. Rename Columns
            # todolist_mission.mission_set_id -> todolist_id
            try:
                print("Renaming column in todolist_mission...")
                # Check if column exists first? Or just try.
                cursor.execute("DESCRIBE todolist_mission")
                cols = [row['Field'] for row in cursor.fetchall()]
                if 'mission_set_id' in cols:
                    cursor.execute("ALTER TABLE todolist_mission CHANGE COLUMN mission_set_id todolist_id BIGINT NOT NULL")
                    print("  -> Done.")
                else:
                    print("  -> 'mission_set_id' column not found (maybe already renamed).")
            except Exception as e:
                print(f"  -> Failed: {e}")

            # todolist_review.mission_set_id -> todolist_id
            try:
                print("Renaming column in todolist_review...")
                cursor.execute("DESCRIBE todolist_review")
                cols = [row['Field'] for row in cursor.fetchall()]
                if 'mission_set_id' in cols:
                    cursor.execute("ALTER TABLE todolist_review CHANGE COLUMN mission_set_id todolist_id BIGINT NOT NULL")
                    print("  -> Done.")
                else:
                    print("  -> 'mission_set_id' column not found (maybe already renamed).")
            except Exception as e:
                print(f"  -> Failed: {e}")

            print("Enabling FK checks...")
            cursor.execute("SET FOREIGN_KEY_CHECKS=1;")
        
        conn.commit()
        print("Migration completed successfully.")

    except Exception as e:
        print(f"Migration error: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    migrate()
