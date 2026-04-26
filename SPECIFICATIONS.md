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
- First fetch the offer price from an Azure Foundry model 
- The model name is 'Phi-4-reasoning-1'
- Add an environment variable for the URL to the model API
- Add an environment variable for the model API key
- Create a system prompt to use with the model. Add an environment variable to keep the system prompt configurable
- Create a templated user prompt with the form data elements as parameters. Add an environment variable to keep the user prompt configurable
- Use the system and user prompt to get the offer price from the model
- Save the form data and offer price to the database. Use same database table as /save-leave-email API