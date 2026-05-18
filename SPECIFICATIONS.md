# Requirements

## Room Cost API
- The /room-cost API reads data from the database
- The database has cost and valid-upto date
- If system date is after valid-upto date, retrun an error

## Save Leave Email API
- The /save-leave-email API saves form data to the database
- The key of the record is a unique form number and email address

## Save Form API
- The /save-form API saves form data to the database
- Use same database table as /save-leave-email API
- Starts a worker thread
- Returns a unique form number

## Worker thread in Save Form API
- The worker thread creates the offer price randomly
- Add environment variables for the minimum and maximum monthly price, default is 75 to 100 EUR
- The annual price is monthly x 12 x discount
- Add environment variable to discount the annual price, default is 0.85
- Save the offer price to the database. Use same database table as /save-form API

## Offer/{UUID} API
- The /offer/{UUID} API fetches the offer from same database table as /save-form API
- Checks if the offer price is available in the database table 
- If price is not availabe, waits 60s
- Checks again if the offer price is available in the database table
- If price is not availabe, returns a 202 allowing the React UI to invoke this API again