openapi: 3.1.0
info:
  title: Arète Multistep Form API
  version: 1.0.0
  description: OBSOLETE. See API-SPEC-2.md. Backend APIs for the Arète health insurance multistep web form.

servers:
  - url: /api/msf
    description: Relative base path

components:
  schemas:
    FormData:
      type: object
      description: All form fields collected across all steps.
      properties:
        profile:
          type: string
          nullable: true
          enum: [employee, self_employed, retired, civil_servant, student]
        coverPartner:
          type: boolean
        coverChildren:
          type: boolean
        numberOfChildren:
          type: integer
          minimum: 0
        age:
          type: string
          description: Subscriber age as a string (18–85).
        postcode:
          type: string
          description: Valid French postcode.
        opticalNeeds:
          type: string
          enum: [nothing, standard, progressive, surgery]
        dentalNeeds:
          type: string
          enum: [none, maintenance, standard, major]
        alternativeMedicine:
          type: string
          enum: [never, one_two, more_than_three]
        hospitalisationPreference:
          type: string
          enum: [shared, private_preferred, private_essential]
        doctorChoice:
          type: string
          nullable: true
          enum: [gp_specialist, specialist_standard, specialist_private]
        email:
          type: string
          format: email
        phoneNumber:
          type: string
          description: French mobile number (10 digits, starts with 06 or 07).

paths:
  /room-cost:
    post:
      summary: Get average daily private room cost
      description: >
        Returns the average daily cost of a private hospital room based on the
        subscriber's geographic zone (derived from postcode) and profile.
      operationId: getRoomCost
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FormData'
      responses:
        '200':
          description: Room cost retrieved successfully.
          content:
            application/json:
              schema:
                type: object
                required: [averageDailyRoomCost, currency]
                properties:
                  averageDailyRoomCost:
                    type: number
                    format: float
                    example: 85.00
                  currency:
                    type: string
                    example: EUR
        '400':
          description: Invalid request body.
        '500':
          description: Internal server error.

  /save-leave-email:
    post:
      summary: Save email for leave-and-come-back
      description: >
        Saves the subscriber's email and current form state so that a resume
        link can be sent by email, allowing them to return and complete the form
        at a later time.
      operationId: saveLeaveEmail
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [email, formData]
              properties:
                email:
                  type: string
                  format: email
                  description: Email address to send the resume link to.
                formData:
                  $ref: '#/components/schemas/FormData'
      responses:
        '204':
          description: Email and form state saved. Resume link will be sent.
        '400':
          description: Invalid email or request body.
        '500':
          description: Internal server error.

  /offer:
    post:
      summary: Get personalised insurance offer
      description: >
        Calculates and returns a personalised monthly and annual premium based
        on the subscriber's complete form responses.
      operationId: getFinalOffer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FormData'
      responses:
        '200':
          description: Offer calculated successfully.
          content:
            application/json:
              schema:
                type: object
                required: [monthlyPremium, annualPremium, currency, coverageDetails]
                properties:
                  monthlyPremium:
                    type: number
                    format: float
                    example: 42.50
                  annualPremium:
                    type: number
                    format: float
                    example: 510.00
                  currency:
                    type: string
                    example: EUR
                  coverageDetails:
                    type: array
                    items:
                      type: string
                    description: Human-readable list of included coverage items.
                    example:
                      - "Optical: Standard glasses or contact lenses"
                      - "Dental: Just maintenance"
                      - "Alternative medicine: 1–2 sessions/year"
        '400':
          description: Invalid or incomplete form data.
        '500':
          description: Internal server error.
