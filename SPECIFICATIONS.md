# Requirements

## Room Cost API
- The /room-cost API reads data from the database
- The database has cost and valid-upto date
- If system date is after valid-upto date, retrun an error

## Save Leave Email API
- The /save-leave-email API saves form data to the database
- The key of the record is a unique form number and email address

## Offer API
- The /offer API calculates the offer
- For now, return 100 Euros per month, and a 1000 Euros pre year 
- Save all form data and offer amounts to the database
- Use same database table as /save-leave-email API