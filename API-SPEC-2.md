openapi: 3.1.0
info:
  title: Arète Multistep Form API
  version: 2.0.0
  description: Backend APIs for the Arète health insurance multistep web form.

servers:
  - url: /api
    description: Relative base path (configure via VITE_API_BASE_URL)

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

    OfferResult:
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

  /save-form:
    post:
      summary: Save completed form and initiate offer calculation
      description: >
        Persists all form responses and triggers the asynchronous offer
        calculation. Returns a UUID that can be used to poll for the result
        via GET /offer/{uuid}.
      operationId: saveForm
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FormData'
      responses:
        '200':
          description: Form saved and offer calculation started.
          content:
            application/json:
              schema:
                type: object
                required: [uuid]
                properties:
                  uuid:
                    type: string
                    format: uuid
                    example: "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        '400':
          description: Invalid or incomplete form data.
        '500':
          description: Internal server error.

  /offer/{uuid}:
    get:
      summary: Retrieve a calculated insurance offer
      description: >
        Returns the personalised monthly and annual premium for the form
        submission identified by the given UUID. This call may take up to
        120 seconds while the AI model computes the offer. The frontend
        retries up to 5 times on timeout before informing the subscriber
        that they will be contacted.
      operationId: getOfferByUuid
      parameters:
        - name: uuid
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: UUID returned by POST /save-form.
      responses:
        '200':
          description: Offer calculated successfully.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OfferResult'
        '404':
          description: No form submission found for the given UUID.
        '500':
          description: Internal server error or model computation failure.
